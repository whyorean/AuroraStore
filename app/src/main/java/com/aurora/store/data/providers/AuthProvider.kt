/*
 * SPDX-FileCopyrightText: 2021 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.providers

import android.app.Activity
import android.content.Context
import android.util.Log
import com.aurora.Constants
import com.aurora.extensions.TAG
import com.aurora.gplayapi.android.AndroidLogger
import com.aurora.gplayapi.device.DeviceProfile
import com.aurora.gplayapi.net.HttpEngine
import com.aurora.gplayapi.session.AuthenticatedSession
import com.aurora.gplayapi.session.SessionFactory
import com.aurora.gplayapi.session.TokenType
import com.aurora.store.AuroraApp
import com.aurora.store.BuildConfig.APPLICATION_ID
import com.aurora.store.BuildConfig.VERSION_CODE
import com.aurora.store.BuildConfig.VERSION_NAME
import com.aurora.store.R
import com.aurora.store.data.AccountRepository
import com.aurora.store.data.model.AccountType
import com.aurora.store.data.model.Auth
import com.aurora.store.data.room.account.Account
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_AUTH_DATA
import com.aurora.store.util.Preferences.PREFERENCE_DISPENSER_URLS
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.Headers.Companion.toHeaders
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

@Singleton
class AuthProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
    private val spoofProvider: SpoofProvider,
    private val okHttpClient: OkHttpClient,
    private val httpEngine: HttpEngine,
    private val accountRepository: AccountRepository,
    private val tokenProvider: GoogleAccountTokenProvider
) {
    private val sessionFactory = SessionFactory(httpEngine, AndroidLogger)

    @Volatile
    private var cachedDefault: Account? = null

    private val _sessionRejections = MutableStateFlow(0)
    val sessionRejections = _sessionRejections.asStateFlow()

    @Volatile
    private var lastSessionRejectionAt = 0L

    @Volatile
    private var previousSessionRejectionAt = 0L

    @Volatile
    private var cachedAuthData: AuthenticatedSession? = null

    @Volatile
    private var cacheWarm = false

    /**
     * Completed by the [init] collector's first emission, or when that collector fails, so
     * [currentSession] can tell "not loaded yet" from "no account".
     */
    private val firstLoad = CompletableDeferred<Unit>()

    init {
        AuroraApp.scope.launch {
            try {
                importLegacyAccountIfNeeded()
                accountRepository.defaultAccount.collect { account ->
                    cachedDefault = account
                    cachedAuthData = decodeAuthData(account)
                    cacheWarm = true
                    firstLoad.complete(Unit)
                }
            } finally {
                firstLoad.complete(Unit)
            }
        }
    }

    val dispenserURL: String?
        get() {
            val dispensers = Preferences.getStringSet(context, PREFERENCE_DISPENSER_URLS)
            return if (dispensers.isNotEmpty()) dispensers.random() else null
        }

    /** Default account's session (or `null`). Served from the cache when warm (see [init]). */
    val authData: AuthenticatedSession?
        get() = if (cacheWarm) cachedAuthData else decodeAuthData(defaultAccountSnapshot())

    /** Whether the default account is anonymous. Served from the cache when warm (see [init]). */
    val isAnonymous: Boolean
        get() = defaultAccountSnapshot()?.isAnonymous ?: true

    /**
     * The default account mapped onto a v4 [AuthenticatedSession], for gplayapi v4's
     * [com.aurora.gplayapi.session.AuthSessionSource] contract - resolved several times per API
     * call and documented there as needing to be cheap: no disk, no network, on every
     * invocation. Reads only the [init] collector's cache, suspending until its first emission:
     * `null` means the default account has no usable session, which
     * [com.aurora.gplayapi.session.DefaultPlaySessions] answers by minting an anonymous one.
     * Answering `null` while the cache is still loading would mint one on every cold start that
     * reaches Play before the account database does, such as a deep link into app details.
     */
    internal suspend fun currentSession(): AuthenticatedSession? {
        firstLoad.await()
        return cachedAuthData
    }

    /**
     * The default account: from the in-memory cache when warm, otherwise a one-off blocking query
     * that also runs the legacy import. The cache is kept current by the [init] collector, so the
     * blocking path only runs on the very first read before the first Flow emission.
     */
    private fun defaultAccountSnapshot(): Account? = if (cacheWarm) {
        cachedDefault
    } else {
        runBlocking {
            importLegacyAccountIfNeeded()
            accountRepository.getDefault()
        }
    }

    /**
     * Decodes [Account.authDataJson] as a v4 [AuthenticatedSession].
     *
     * A session persisted before this migration is v3 `AuthData` JSON, a different shape v4
     * cannot read - caught here rather than propagated, and treated the same as "no session":
     * [AuthViewModel]'s existing invalid-session path already re-mints from the plain
     * email/token this class keeps redundantly ([AccountProvider]'s prefs mirror for the
     * default account, [Account.aasToken]/[Account.authToken] for every other one via
     * [refresh]), so this is a one-time, automatic re-login on first use after upgrading,
     * never a hard failure.
     */
    private fun decodeAuthData(account: Account?): AuthenticatedSession? =
        account?.authDataJson?.takeIf { it.isNotBlank() }?.let {
            try {
                json.decodeFromString<AuthenticatedSession>(it)
            } catch (exception: SerializationException) {
                Log.e(
                    TAG,
                    "Stored session for ${account.id} is not a valid v4 session; treating as signed out",
                    exception
                )
                null
            }
        }

    fun recordSessionRejection() {
        previousSessionRejectionAt = lastSessionRejectionAt
        lastSessionRejectionAt = System.currentTimeMillis()
        _sessionRejections.update { it + 1 }
    }

    fun clearSessionRejections() {
        previousSessionRejectionAt = 0L
        lastSessionRejectionAt = 0L
    }

    val isSessionRepeatedlyRejected: Boolean
        get() = previousSessionRejectionAt != 0L &&
            lastSessionRejectionAt - previousSessionRejectionAt < SESSION_REJECTION_WINDOW_MS &&
            System.currentTimeMillis() - lastSessionRejectionAt < SESSION_REJECTION_WINDOW_MS

    /**
     * Checks whether saved session is valid or not
     * @throws IOException when Play can't be reached, instead of reporting the session invalid
     */
    suspend fun isSavedAuthDataValid(): Boolean {
        val session = authData ?: return false
        if (!matchesActiveSpoof(session.device, session.locale)) return false
        return sessionFactory.validate(session)
    }

    /**
     * Whether a session built under [device]/[locale] matches the device configuration and
     * locale active right now.
     *
     * Play picks the split APKs it serves from the device configuration uploaded when the
     * session was created, so a session built under a different device keeps getting that
     * device's splits however the spoof is set afterwards. Such a session has to be rebuilt
     * before it is used. Compares the whole [DeviceProfile.fingerprint] rather than a
     * handful of hand-picked keys, which is stricter and, unlike v3's field-by-field
     * comparison, also catches a difference in a custom-imported config that those specific
     * keys never looked at.
     */
    fun matchesActiveSpoof(device: DeviceProfile?, locale: Locale): Boolean {
        device ?: return false
        return locale == spoofProvider.locale &&
            device.fingerprint == spoofProvider.deviceProfile().fingerprint
    }

    /**
     * Builds an [AuthenticatedSession] for login using personal Google account
     * @param email E-mail ID
     * @param token AAS or Auth token
     * @param tokenType Type of the token, one from [TokenType]
     * @return Result encapsulating [AuthenticatedSession] or exception
     */
    suspend fun buildGoogleAuthData(
        email: String,
        token: String,
        tokenType: TokenType
    ): Result<AuthenticatedSession> {
        return withContext(Dispatchers.IO) {
            try {
                return@withContext Result.success(
                    sessionFactory.authenticate(
                        email = email,
                        token = token,
                        tokenType = tokenType,
                        device = spoofProvider.deviceProfile(),
                        locale = spoofProvider.locale
                    )
                )
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to generate Session", exception)
                return@withContext Result.failure(exception)
            }
        }
    }

    /**
     * Builds an [AuthenticatedSession] for login using one of the dispensers
     * @return Result encapsulating [AuthenticatedSession] or exception
     */
    suspend fun buildAnonymousAuthData(): Result<AuthenticatedSession> {
        return withContext(Dispatchers.IO) {
            try {
                val body = postToDispenser(
                    dispenserURL!!,
                    json.encodeToString(spoofProvider.deviceProperties).toByteArray()
                ).use { response ->
                    if (!response.isSuccessful) throwError(response, context)
                    response.body.string()
                }

                val auth = json.decodeFromString<Auth>(body)
                val session = sessionFactory.authenticate(
                    email = auth.email,
                    token = auth.auth,
                    tokenType = TokenType.AUTH,
                    device = spoofProvider.deviceProfile(),
                    locale = spoofProvider.locale
                ).copy(isDispensed = true)
                return@withContext Result.success(session)
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to generate AuthData", exception)
                return@withContext Result.failure(exception)
            }
        }
    }

    @Throws(IOException::class)
    private fun postToDispenser(url: String, body: ByteArray): Response {
        val headers = mapOf(
            "User-Agent" to "${APPLICATION_ID}-${VERSION_NAME}-${VERSION_CODE}"
        ).toHeaders()
        val request = Request(
            url = url.toHttpUrl(),
            headers = headers,
            method = "POST",
            body = body.toRequestBody("application/json".toMediaType())
        )
        return okHttpClient.newCall(request).execute()
    }

    /**
     * Returns the [AuthenticatedSession] for the given [accountId], or null if not found / not
     * yet cached.
     */
    suspend fun getAuthData(accountId: String): AuthenticatedSession? =
        decodeAuthData(accountRepository.getById(accountId))

    /**
     * The gplayapi v4 session for [accountId], refreshed first when it is missing, was built
     * under a device spoof that is no longer active, or is no longer accepted by Play. Play
     * serves the split APKs of whatever device a session was created under, so a stale session
     * quietly delivers another device's splits rather than failing.
     *
     * Refuses rather than falling back to another account: this exists for an app bound to a
     * specific account, and resolving that app as the default account would acquire it under an
     * identity the user did not pick.
     *
     * Unlike [currentSession] this is deliberately *not* cheap - it can hit the database, the
     * network and a token mint - so it belongs on a per-download path, never behind
     * `AuthSessionSource.current`.
     *
     * @throws IllegalStateException when the account cannot produce a usable session
     */
    suspend fun sessionFor(accountId: String): AuthenticatedSession {
        val stored = getAuthData(accountId)
            ?.takeIf { matchesActiveSpoof(it.device, it.locale) }
        if (stored != null && sessionFactory.validate(stored)) return stored

        return refresh(accountId).getOrElse { error ->
            throw IllegalStateException(
                "Could not refresh session for account $accountId; re-authentication required",
                error
            )
        }
    }

    /**
     * Rebuilds and persists [accountId]'s session. Pass an [activity] for user-initiated
     * refreshes so a device (microG) account can show any consent UI it needs; background
     * callers omit it and rely on a non-interactive token mint.
     */
    suspend fun refresh(
        accountId: String,
        activity: Activity? = null
    ): Result<AuthenticatedSession> {
        val account = accountRepository.getById(accountId)
            ?: return Result.failure(IllegalStateException("No account $accountId"))
        return runCatching {
            val authData = when (account.type) {
                AccountType.ANONYMOUS -> buildAnonymousAuthData().getOrThrow()
                AccountType.GOOGLE -> when (account.tokenType) {
                    TokenType.AAS -> {
                        val aasToken = account.aasToken
                            ?: throw IllegalStateException(
                                "AAS account ${account.id} has no aasToken"
                            )
                        buildGoogleAuthData(account.email, aasToken, TokenType.AAS)
                            .getOrThrow()
                    }
                    TokenType.AUTH -> {
                        val token = tokenProvider.fetchAuthToken(
                            account.email,
                            account.authToken,
                            activity
                        )
                        buildGoogleAuthData(account.email, token, TokenType.AUTH).getOrThrow()
                    }
                }
            }
            accountRepository.upsertAccount(
                account.copy(
                    email = authData.email,
                    authDataJson = json.encodeToString(authData),
                    displayName = authData.userProfile?.name ?: account.displayName,
                    profilePicUrl = authData.userProfile?.artwork?.url ?: account.profilePicUrl
                )
            )
            authData
        }
    }

    /** Refreshes the target account, then marks it default. Leaves the old default if it fails. */
    suspend fun setDefaultAccount(accountId: String): Result<Unit> = refresh(accountId).map {
        accountRepository.setDefault(accountId)
        syncDefaultToPrefs()
    }

    /**
     * Mirrors the current DB default account into the legacy single-account prefs. The login gate
     * (`AccountProvider.isLoggedIn`) and the session-refresh paths still read those prefs, so this
     * must be called after every change of *which* account is the default (login, set-default,
     * account removal) to keep them in sync with the DB. Clears the prefs when no default remains.
     */
    suspend fun syncDefaultToPrefs() {
        val account = accountRepository.getDefault()
        if (account == null) {
            AccountProvider.logout(context)
            return
        }
        val token = account.aasToken ?: account.authToken.orEmpty()
        AccountProvider.login(context, account.email, token, account.tokenType, account.type)
        account.authDataJson?.let { Preferences.putString(context, PREFERENCE_AUTH_DATA, it) }
        Preferences.putBoolean(
            context,
            Preferences.PREFERENCE_AUTH_VIA_MICROG,
            account.authViaMicroG
        )
    }

    /**
     * Persists [authData] for the given account into the account table so the DB-backed
     * [authData] stays current. Upserts the row (id derived from type+email) and, when
     * [makeDefault] is true, marks it the default.
     */
    suspend fun persistAccount(
        authData: AuthenticatedSession,
        accountType: AccountType,
        authViaMicroG: Boolean,
        makeDefault: Boolean = true
    ) {
        val tokenType = if (authData.aasToken.isNullOrBlank()) TokenType.AUTH else TokenType.AAS
        val account = Account(
            id = Account.idFor(accountType, authData.email),
            type = accountType,
            email = authData.email,
            displayName = authData.userProfile?.name,
            profilePicUrl = authData.userProfile?.artwork?.url,
            aasToken = authData.aasToken?.ifBlank { null },
            authToken = authData.authToken.ifBlank { null },
            tokenType = tokenType,
            authViaMicroG = authViaMicroG,
            authDataJson = json.encodeToString(authData),
            isDefault = false
        )
        accountRepository.upsertAccount(account)
        if (makeDefault) {
            accountRepository.setDefault(account.id)
            syncDefaultToPrefs()
        }
    }

    /**
     * Full sign-out: removes every account from the DB and clears the legacy single-account
     * prefs. Used by the auth-failure / reset paths so a stale account row can't linger as the
     * DB is now the source of truth. Uses `runBlocking` (see [authData]); call sparingly.
     */
    fun logout() = runBlocking {
        accountRepository.getAll().forEach { accountRepository.removeAccount(it.id) }
        AccountProvider.logout(context)
    }

    /**
     * Saves given [AuthenticatedSession]
     */
    fun saveAuthData(authData: AuthenticatedSession) {
        Preferences.putString(context, PREFERENCE_AUTH_DATA, json.encodeToString(authData))
    }

    /**
     * Removes saved session
     */
    fun removeAuthData(context: Context) {
        Preferences.remove(context, PREFERENCE_AUTH_DATA)
    }

    private suspend fun importLegacyAccountIfNeeded() {
        if (accountRepository.getAll().isNotEmpty()) return
        if (!Preferences.getBoolean(context, Constants.ACCOUNT_SIGNED_IN, false)) return

        val type = if (Preferences.getString(context, Constants.ACCOUNT_TYPE) == "GOOGLE") {
            AccountType.GOOGLE
        } else {
            AccountType.ANONYMOUS
        }
        val email = Preferences.getString(context, Constants.ACCOUNT_EMAIL_PLAIN)
        val aas = Preferences.getString(context, Constants.ACCOUNT_AAS_PLAIN).ifBlank { null }
        val auth = Preferences.getString(context, Constants.ACCOUNT_AUTH_PLAIN).ifBlank { null }
        val rawAuth = Preferences.getString(context, PREFERENCE_AUTH_DATA).ifBlank { null }

        accountRepository.upsertAccount(
            Account(
                id = Account.idFor(type, email),
                type = type,
                email = email.ifBlank { "anonymous@aurora" },
                aasToken = aas,
                authToken = auth,
                tokenType = if (aas != null) TokenType.AAS else TokenType.AUTH,
                authViaMicroG = Preferences.getBoolean(
                    context,
                    Preferences.PREFERENCE_AUTH_VIA_MICROG,
                    false
                ),
                authDataJson = rawAuth,
                isDefault = true
            )
        )
    }

    @Throws(Exception::class)
    private fun throwError(response: Response, context: Context) {
        when (response.code) {
            400 -> throw Exception(context.getString(R.string.bad_request))

            403 -> throw Exception(context.getString(R.string.access_denied_using_vpn))

            404 -> throw Exception(context.getString(R.string.server_unreachable))

            429 -> throw Exception(context.getString(R.string.login_rate_limited))

            503 -> throw Exception(context.getString(R.string.server_maintenance))

            else -> {
                if (response.message.isNotBlank()) {
                    throw Exception(response.message)
                } else {
                    throw Exception(
                        context.getString(
                            R.string.failed_generating_session,
                            response.code
                        )
                    )
                }
            }
        }
    }

    companion object {
        const val SESSION_REJECTION_WINDOW_MS = 60_000L
    }
}
