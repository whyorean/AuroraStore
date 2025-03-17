package com.aurora.store.data.work

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AccountManagerCallback
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import androidx.core.os.bundleOf
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.helpers.AuthHelper
import com.aurora.store.data.model.AccountType
import com.aurora.store.data.providers.AccountProvider
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.util.CertUtil.GOOGLE_ACCOUNT_TYPE
import com.aurora.store.util.CertUtil.GOOGLE_PLAY_AUTH_TOKEN_TYPE
import com.aurora.store.util.CertUtil.GOOGLE_PLAY_CERT
import com.aurora.store.util.CertUtil.GOOGLE_PLAY_PACKAGE_NAME
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Worker to refresh [AuthData] in background
 * @see UpdateWorker
 */
@HiltWorker
open class AuthWorker @AssistedInject constructor(
    private val authProvider: AuthProvider,
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val TAG = AuthWorker::class.java.simpleName

    override suspend fun doWork(): Result {
        if (!AccountProvider.isLoggedIn(context)) {
            Log.i(TAG, "User has logged out!")
            return Result.failure()
        }

        if (authProvider.isSavedAuthDataValid()) {
            Log.i(TAG, "Saved AuthData is valid")
            return Result.success()
        }

        // Generate and validate new auth
        try {
            val accountType = AccountProvider.getAccountType(context)
            val authData = when (accountType) {
                AccountType.GOOGLE -> {
                    val email = AccountProvider.getLoginEmail(context)
                    val tokenPair = AccountProvider.getLoginToken(context)

                    if (email == null || tokenPair == null) {
                        throw Exception()
                    }

                    when (tokenPair.second) {
                        AuthHelper.Token.AAS -> {
                            Log.i(TAG, "Refreshing AuthData for personal account")
                            authProvider.buildGoogleAuthData(
                                email,
                                tokenPair.first,
                                AuthHelper.Token.AAS
                            ).getOrThrow()
                        }

                        AuthHelper.Token.AUTH -> {
                            Log.i(
                                TAG,
                                "Refreshing AuthData for personal account using AccountManager"
                            )
                            val newToken = fetchAuthToken(email, tokenPair.first)
                            authProvider.buildGoogleAuthData(email, newToken, AuthHelper.Token.AAS)
                                .getOrThrow()
                        }
                    }
                }

                AccountType.ANONYMOUS -> {
                    Log.i(TAG, "Refreshing AuthData for anonymous account")
                    authProvider.buildAnonymousAuthData().getOrThrow()
                }
            }

            require(verifyAndSaveAuth(authData, accountType) != null)
            Log.i(TAG, "Refreshed AuthData successfully")
            return Result.success()
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to refresh authData!", exception)
            return Result.failure()
        }
    }

    private suspend fun fetchAuthToken(email: String, oldToken: String? = null): String {
        return suspendCoroutine { continuation ->
            fetchAuthToken(email, oldToken) { future ->
                try {
                    val bundle = future.result
                    val token = bundle.getString(AccountManager.KEY_AUTHTOKEN)

                    if (token != null) {
                        continuation.resume(token)
                    } else {
                        continuation.resumeWithException(IllegalStateException("Auth token is null"))
                    }
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }
        }
    }

    private fun fetchAuthToken(
        email: String,
        oldToken: String? = null,
        callback: AccountManagerCallback<Bundle>
    ) {
        try {
            if (oldToken != null) {
                // Invalidate the old token before requesting a new one
                AccountManager.get(context)
                    .invalidateAuthToken(
                        GOOGLE_ACCOUNT_TYPE,
                        oldToken
                    )
            }

            AccountManager.get(context)
                .getAuthToken(
                    Account(email, GOOGLE_ACCOUNT_TYPE),
                    GOOGLE_PLAY_AUTH_TOKEN_TYPE,
                    bundleOf(
                        "overridePackage" to GOOGLE_PLAY_PACKAGE_NAME,
                        "overrideCertificate" to Base64.decode(
                            GOOGLE_PLAY_CERT,
                            Base64.DEFAULT
                        )
                    ),
                    true,
                    callback,
                    Handler(Looper.getMainLooper())
                )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch auth token", e)
            callback.run(null)
        }
    }

    private fun verifyAndSaveAuth(authData: AuthData, accountType: AccountType): AuthData? {
        return if (authData.authToken.isNotEmpty() && authData.deviceConfigToken.isNotEmpty()) {
            authProvider.saveAuthData(authData)
            AccountProvider.login(
                context,
                authData.email,
                authData.aasToken.ifBlank { authData.authToken },
                if (authData.aasToken.isBlank()) AuthHelper.Token.AUTH else AuthHelper.Token.AAS,
                accountType
            )
            authData
        } else {
            authProvider.removeAuthData(context)
            AccountProvider.logout(context)
            null
        }
    }
}
