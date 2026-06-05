/*
 * Aurora Store
 *  Copyright (C) 2021, Rahul Kumar Patel <whyorean@gmail.com>
 *
 *  Aurora Store is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Aurora Store is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.aurora.store.data.providers

import android.content.Context
import android.util.Log
import com.aurora.Constants
import com.aurora.extensions.TAG
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.data.models.PlayResponse
import com.aurora.gplayapi.helpers.AuthHelper
import com.aurora.gplayapi.network.IHttpClient
import com.aurora.store.AuroraApp
import com.aurora.store.R
import com.aurora.store.data.AccountRepository
import com.aurora.store.data.model.AccountType
import com.aurora.store.data.model.Auth
import com.aurora.store.data.room.account.Account
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_AUTH_DATA
import com.aurora.store.util.Preferences.PREFERENCE_DISPENSER_URLS
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

@Singleton
class AuthProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
    private val spoofProvider: SpoofProvider,
    private val httpClient: IHttpClient,
    private val accountRepository: AccountRepository,
    private val tokenProvider: GoogleAccountTokenProvider
) {
    @Volatile
    private var cachedDefault: Account? = null

    @Volatile
    private var cacheWarm = false

    init {
        AuroraApp.scope.launch {
            importLegacyAccountIfNeeded()
            accountRepository.defaultAccount.collect { account ->
                cachedDefault = account
                cacheWarm = true
            }
        }
    }

    val dispenserURL: String?
        get() {
            val dispensers = Preferences.getStringSet(context, PREFERENCE_DISPENSER_URLS)
            return if (dispensers.isNotEmpty()) dispensers.random() else null
        }

    /** Default account's AuthData (or BOGUS). Served from the cache when warm (see [init]). */
    val authData: AuthData?
        get() = decodeAuthData(defaultAccountSnapshot())

    /** Whether the default account is anonymous. Served from the cache when warm (see [init]). */
    val isAnonymous: Boolean
        get() = defaultAccountSnapshot()?.isAnonymous ?: true

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

    private fun decodeAuthData(account: Account?): AuthData =
        account?.authDataJson?.takeIf { it.isNotBlank() }
            ?.let { json.decodeFromString<AuthData>(it) }
            ?: AuthData("BOGUS")

    /**
     * Checks whether saved AuthData is valid or not
     */
    fun isSavedAuthDataValid(): Boolean = AuthHelper.isValid(authData!!)

    /**
     * Builds [AuthData] for login using personal Google account
     * @param email E-mail ID
     * @param token AAS or Auth token
     * @param tokenType Type of the token, one from [AuthHelper.Token]
     * @return Result encapsulating [AuthData] or exception
     */
    suspend fun buildGoogleAuthData(
        email: String,
        token: String,
        tokenType: AuthHelper.Token
    ): Result<AuthData> {
        return withContext(Dispatchers.IO) {
            try {
                return@withContext Result.success(
                    AuthHelper.build(
                        email = email,
                        token = token,
                        tokenType = tokenType,
                        properties = spoofProvider.deviceProperties,
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
     * Builds [AuthData] for login using one of the dispensers
     * @return Result encapsulating [AuthData] or exception
     */
    suspend fun buildAnonymousAuthData(): Result<AuthData> {
        return withContext(Dispatchers.IO) {
            try {
                val playResponse = httpClient.postAuth(
                    dispenserURL!!,
                    json.encodeToString(spoofProvider.deviceProperties).toByteArray()
                ).also {
                    if (!it.isSuccessful) throwError(it, context)
                }

                val auth = json.decodeFromString<Auth>(String(playResponse.responseBytes))
                return@withContext Result.success(
                    AuthHelper.build(
                        email = auth.email,
                        token = auth.auth,
                        tokenType = AuthHelper.Token.AUTH,
                        isAnonymous = true,
                        properties = spoofProvider.deviceProperties,
                        locale = spoofProvider.locale
                    )
                )
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to generate AuthData", exception)
                return@withContext Result.failure(exception)
            }
        }
    }

    /**
     * Returns the [AuthData] for the given [accountId], or null if not found / not yet cached.
     */
    suspend fun getAuthData(accountId: String): AuthData? {
        val account = accountRepository.getById(accountId) ?: return null
        return account.authDataJson?.takeIf { it.isNotBlank() }
            ?.let { json.decodeFromString<AuthData>(it) }
    }

    /** Rebuilds and persists [accountId]'s AuthData. */
    suspend fun refresh(accountId: String): Result<AuthData> {
        val account = accountRepository.getById(accountId)
            ?: return Result.failure(IllegalStateException("No account $accountId"))
        return runCatching {
            val authData = when (account.type) {
                AccountType.ANONYMOUS -> buildAnonymousAuthData().getOrThrow()
                AccountType.GOOGLE -> when (account.tokenType) {
                    AuthHelper.Token.AAS -> {
                        val aasToken = account.aasToken
                            ?: throw IllegalStateException(
                                "AAS account ${account.id} has no aasToken"
                            )
                        buildGoogleAuthData(account.email, aasToken, AuthHelper.Token.AAS)
                            .getOrThrow()
                    }
                    AuthHelper.Token.AUTH -> {
                        val token = tokenProvider.fetchAuthToken(account.email, account.authToken)
                        buildGoogleAuthData(account.email, token, AuthHelper.Token.AAS).getOrThrow()
                    }
                }
            }
            accountRepository.upsertAccount(
                account.copy(
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
        authData: AuthData,
        accountType: AccountType,
        authViaMicroG: Boolean,
        makeDefault: Boolean = true
    ) {
        val tokenType =
            if (authData.aasToken.isBlank()) AuthHelper.Token.AUTH else AuthHelper.Token.AAS
        val account = Account(
            id = Account.idFor(accountType, authData.email),
            type = accountType,
            email = authData.email,
            displayName = authData.userProfile?.name,
            profilePicUrl = authData.userProfile?.artwork?.url,
            aasToken = authData.aasToken.ifBlank { null },
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
     * Saves given [AuthData]
     */
    fun saveAuthData(authData: AuthData) {
        Preferences.putString(context, PREFERENCE_AUTH_DATA, json.encodeToString(authData))
    }

    /**
     * Removes saved [AuthData]
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
                tokenType = if (aas != null) AuthHelper.Token.AAS else AuthHelper.Token.AUTH,
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
    private fun throwError(playResponse: PlayResponse, context: Context) {
        when (playResponse.code) {
            400 -> throw Exception(context.getString(R.string.bad_request))

            403 -> throw Exception(context.getString(R.string.access_denied_using_vpn))

            404 -> throw Exception(context.getString(R.string.server_unreachable))

            429 -> throw Exception(context.getString(R.string.login_rate_limited))

            503 -> throw Exception(context.getString(R.string.server_maintenance))

            else -> {
                if (playResponse.errorString.isNotBlank()) {
                    throw Exception(playResponse.errorString)
                } else {
                    throw Exception(
                        context.getString(
                            R.string.failed_generating_session,
                            playResponse.code
                        )
                    )
                }
            }
        }
    }
}
