package com.aurora.store.data.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aurora.extensions.TAG
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.helpers.AuthHelper
import com.aurora.store.data.model.AccountType
import com.aurora.store.data.providers.AccountProvider
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.data.providers.GoogleAccountTokenProvider
import com.aurora.store.util.Preferences
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Worker to refresh [AuthData] in background
 * @see UpdateWorker
 */
@HiltWorker
open class AuthWorker @AssistedInject constructor(
    private val authProvider: AuthProvider,
    private val tokenProvider: GoogleAccountTokenProvider,
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

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
                            Log.i(TAG, "Refreshing AuthData via AccountManager")
                            val newToken = tokenProvider.fetchAuthToken(email, tokenPair.first)
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
            authProvider.persistAccount(
                authData = authData,
                accountType = accountType,
                authViaMicroG = Preferences.getBoolean(
                    context,
                    Preferences.PREFERENCE_AUTH_VIA_MICROG,
                    false
                ),
                makeDefault = true
            )
            Log.i(TAG, "Refreshed AuthData successfully")
            return Result.success()
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to refresh authData!", exception)
            return Result.failure()
        }
    }

    private fun verifyAndSaveAuth(authData: AuthData, accountType: AccountType): AuthData? =
        if (authData.authToken.isNotEmpty() && authData.deviceConfigToken.isNotEmpty()) {
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
            authProvider.logout()
            null
        }
}
