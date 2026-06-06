/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.providers

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AccountManagerCallback
import android.app.Activity
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
import kotlinx.coroutines.suspendCancellableCoroutine

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

    /**
     * Mints a Play Store AUTH token for [email]. Pass an [activity] for interactive requests
     * (e.g. adding an account) so microG can show any consent UI it needs; omit it for
     * background refreshes, which fall back to the non-interactive overload.
     */
    suspend fun fetchAuthToken(
        email: String,
        oldToken: String? = null,
        activity: Activity? = null
    ): String = suspendCancellableCoroutine { continuation ->
        fetchAuthToken(email, oldToken, activity) { future ->
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
        activity: Activity?,
        callback: AccountManagerCallback<Bundle>
    ) {
        try {
            val accountManager = AccountManager.get(context)
            if (oldToken != null) {
                accountManager.invalidateAuthToken(GOOGLE_ACCOUNT_TYPE, oldToken)
            }
            val account = Account(email, GOOGLE_ACCOUNT_TYPE)
            val options = Bundle().apply {
                putString("overridePackage", PACKAGE_NAME_PLAY_STORE)
                putByteArray(
                    "overrideCertificate",
                    Base64.decode(GOOGLE_PLAY_CERT, Base64.DEFAULT)
                )
            }
            val handler = Handler(Looper.getMainLooper())
            if (activity != null) {
                accountManager.getAuthToken(
                    account,
                    GOOGLE_PLAY_AUTH_TOKEN_TYPE,
                    options,
                    activity,
                    callback,
                    handler
                )
            } else {
                accountManager.getAuthToken(
                    account,
                    GOOGLE_PLAY_AUTH_TOKEN_TYPE,
                    options,
                    true,
                    callback,
                    handler
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch auth token", e)
            callback.run(null)
        }
    }
}
