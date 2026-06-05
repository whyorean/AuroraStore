/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.providers

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AccountManagerCallback
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import com.aurora.Constants.PACKAGE_NAME_PLAY_STORE
import com.aurora.extensions.TAG
import com.aurora.store.util.CertUtil.GOOGLE_ACCOUNT_TYPE
import com.aurora.store.util.CertUtil.GOOGLE_PLAY_AUTH_TOKEN_TYPE
import com.aurora.store.util.CertUtil.GOOGLE_PLAY_CERT
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Wraps the system [AccountManager] for Google/microG accounts: enumerating the accounts present
 * on-device and minting Play Store AUTH tokens for them.
 */
@Singleton
class GoogleAccountTokenProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /** E-mails of Google accounts present on-device (e.g. provided by microG). */
    fun systemGoogleAccountEmails(): List<String> = try {
        AccountManager.get(context).getAccountsByType(GOOGLE_ACCOUNT_TYPE)
            .map { it.name }
            .distinct()
    } catch (e: Exception) {
        Log.e(TAG, "Failed to enumerate system Google accounts", e)
        emptyList()
    }

    suspend fun fetchAuthToken(email: String, oldToken: String? = null): String =
        suspendCoroutine { continuation ->
            fetchAuthToken(email, oldToken) { future ->
                try {
                    val token = future.result.getString(AccountManager.KEY_AUTHTOKEN)
                    if (token != null) {
                        continuation.resume(token)
                    } else {
                        continuation.resumeWithException(
                            IllegalStateException("Auth token is null")
                        )
                    }
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }
        }

    private fun fetchAuthToken(
        email: String,
        oldToken: String?,
        callback: AccountManagerCallback<Bundle>
    ) {
        try {
            if (oldToken != null) {
                AccountManager.get(context).invalidateAuthToken(GOOGLE_ACCOUNT_TYPE, oldToken)
            }
            AccountManager.get(context).getAuthToken(
                Account(email, GOOGLE_ACCOUNT_TYPE),
                GOOGLE_PLAY_AUTH_TOKEN_TYPE,
                Bundle().apply {
                    putString("overridePackage", PACKAGE_NAME_PLAY_STORE)
                    putByteArray(
                        "overrideCertificate",
                        Base64.decode(GOOGLE_PLAY_CERT, Base64.DEFAULT)
                    )
                },
                true,
                callback,
                Handler(Looper.getMainLooper())
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch auth token", e)
            callback.run(null)
        }
    }
}
