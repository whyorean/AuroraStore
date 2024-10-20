package com.aurora.store.data.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.helpers.AuthHelper
import com.aurora.store.data.model.AccountType
import com.aurora.store.data.providers.AccountProvider
import com.aurora.store.data.providers.AuthProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

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
                    authProvider.buildGoogleAuthData(
                        AccountProvider.getLoginEmail(appContext)!!,
                        AccountProvider.getLoginToken(appContext)!!.first,
                        AccountProvider.getLoginToken(appContext)!!.second
                    ).getOrThrow()
                }

                AccountType.ANONYMOUS -> authProvider.buildAnonymousAuthData().getOrThrow()
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
