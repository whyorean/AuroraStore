package com.aurora.store.data.work

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking

/**
 * Worker to refresh [AuthData] in background
 * @see UpdateWorker
 */
@HiltWorker
open class AuthWorker @AssistedInject constructor(
    private val authProvider: AuthProvider,
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val TAG = AuthWorker::class.java.simpleName

    private val authToken: MutableSharedFlow<String?> = MutableSharedFlow(extraBufferCapacity = 1)

    override suspend fun doWork(): Result {
        if (!AccountProvider.isLoggedIn(appContext)) {
            Log.i(TAG, "User has logged out!")
            return Result.failure()
        }

        if (authProvider.isSavedAuthDataValid()) {
            Log.i(TAG, "Saved AuthData is valid")
            return Result.success()
        }

        // Generate and validate new auth
        try {
            val accountType = AccountProvider.getAccountType(appContext)
            val authData = when (accountType) {
                AccountType.GOOGLE -> {
                    val email = AccountProvider.getLoginEmail(appContext)!!
                    val token = AccountProvider.getLoginToken(appContext)!!.first
                    val tokenType = AccountProvider.getLoginToken(appContext)!!.second

                    if (tokenType == AuthHelper.Token.AAS) {
                        Log.i(TAG, "Refreshing AuthData for personal account")
                        authProvider.buildGoogleAuthData(email, token, tokenType).getOrThrow()
                    } else {
                        /*
                         * We are working with AuthToken here. The only scenario when we will have
                         * AuthToken and Google login is when the user used microG to login into
                         * Aurora Store. In this case, we use system's AccountManager to request credentials.
                         */
                        Log.i(TAG, "Refreshing AuthData for personal account using AccountManager")
                        AccountManager.get(appContext)
                            .getAuthToken(
                                Account(email, GOOGLE_ACCOUNT_TYPE),
                                GOOGLE_PLAY_AUTH_TOKEN_TYPE,
                                bundleOf(
                                    "overridePackage" to GOOGLE_PLAY_PACKAGE_NAME,
                                    "overrideCertificate" to Base64.decode(GOOGLE_PLAY_CERT, Base64.DEFAULT)
                                ),
                                true,
                                {
                                    authToken.tryEmit(it.result.getString(AccountManager.KEY_AUTHTOKEN))
                                },
                                Handler(Looper.getMainLooper())
                            )
                        runBlocking {
                            authProvider.buildGoogleAuthData(
                                email,
                                authToken.take(1).first()!!,
                                tokenType
                            ).getOrThrow()
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

    private fun verifyAndSaveAuth(authData: AuthData, accountType: AccountType): AuthData? {
        return if (authData.authToken.isNotEmpty() && authData.deviceConfigToken.isNotEmpty()) {
            authProvider.saveAuthData(authData)
            AccountProvider.login(
                appContext,
                authData.email,
                authData.aasToken.ifBlank { authData.authToken },
                if (authData.aasToken.isBlank()) AuthHelper.Token.AUTH else AuthHelper.Token.AAS,
                accountType
            )
            authData
        } else {
            authProvider.removeAuthData(appContext)
            AccountProvider.logout(appContext)
            null
        }
    }
}
