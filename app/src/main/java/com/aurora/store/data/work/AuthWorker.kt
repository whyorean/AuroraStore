/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aurora.extensions.TAG
import com.aurora.gplayapi.session.AuthenticatedSession
import com.aurora.gplayapi.session.TokenType
import com.aurora.store.data.model.AccountType
import com.aurora.store.data.providers.AccountProvider
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.data.providers.GoogleAccountTokenProvider
import com.aurora.store.util.Preferences
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException

/**
 * Worker to refresh [AuthenticatedSession] in background
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

        val isValid = try {
            authProvider.isSavedAuthDataValid()
        } catch (exception: IOException) {
            Log.e(TAG, "Couldn't reach Play to validate AuthData", exception)
            return Result.failure()
        }

        if (isValid) {
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
                        TokenType.AAS -> {
                            Log.i(TAG, "Refreshing AuthData for personal account")
                            authProvider.buildGoogleAuthData(
                                email,
                                tokenPair.first,
                                TokenType.AAS
                            ).getOrThrow()
                        }

                        TokenType.AUTH -> {
                            Log.i(TAG, "Refreshing AuthData via AccountManager")
                            val newToken = tokenProvider.fetchAuthToken(email, tokenPair.first)
                            authProvider.buildGoogleAuthData(email, newToken, TokenType.AUTH)
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

    private fun verifyAndSaveAuth(
        authData: AuthenticatedSession,
        accountType: AccountType
    ): AuthenticatedSession? =
        if (authData.authToken.isNotEmpty() && authData.deviceConfigToken.isNotEmpty()) {
            authProvider.saveAuthData(authData)
            AccountProvider.login(
                context,
                authData.email,
                authData.aasToken?.ifBlank { authData.authToken } ?: authData.authToken,
                if (authData.aasToken.isNullOrBlank()) TokenType.AUTH else TokenType.AAS,
                accountType
            )
            authData
        } else {
            authProvider.removeAuthData(context)
            authProvider.logout()
            null
        }
}
