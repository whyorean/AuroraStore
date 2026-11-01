/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.providers

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.BundleCompat
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

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
     * (e.g. adding an account) so microG can show any consent UI it needs; without one a request
     * that needs consent fails and the system posts a notification instead.
     */
    suspend fun fetchAuthToken(
        email: String,
        oldToken: String? = null,
        activity: Activity? = null
    ): String {
        val result = requestAuthToken(email, oldToken, notifyAuthFailure = activity == null)
        result.getString(AccountManager.KEY_AUTHTOKEN)?.let { return it }

        val consentIntent = BundleCompat.getParcelable(
            result,
            AccountManager.KEY_INTENT,
            Intent::class.java
        )
        val componentActivity = activity as? ComponentActivity
        if (consentIntent == null || componentActivity == null) {
            throw IllegalStateException("Auth token is null")
        }

        withContext(Dispatchers.Main) { awaitConsent(componentActivity, consentIntent) }
        return requestAuthToken(email, null, notifyAuthFailure = false)
            .getString(AccountManager.KEY_AUTHTOKEN)
            ?: throw IllegalStateException("Auth token is null")
    }

    // microG can return a non-exported consent activity (GmsCore#3853); launch it here so a
    // denied start surfaces instead of AccountManager's callback never firing
    private suspend fun awaitConsent(activity: ComponentActivity, intent: Intent) =
        suspendCancellableCoroutine { continuation ->
            var launcher: ActivityResultLauncher<Intent>? = null
            launcher = activity.activityResultRegistry.register(
                "microg-consent-${System.nanoTime()}",
                ActivityResultContracts.StartActivityForResult()
            ) {
                launcher?.unregister()
                if (continuation.isActive) continuation.resume(Unit)
            }
            continuation.invokeOnCancellation { launcher.unregister() }
            try {
                launcher.launch(intent)
            } catch (exception: SecurityException) {
                launcher.unregister()
                continuation.resumeWithException(exception)
            }
        }

    private suspend fun requestAuthToken(
        email: String,
        oldToken: String?,
        notifyAuthFailure: Boolean
    ): Bundle = suspendCancellableCoroutine { continuation ->
        try {
            val accountManager = AccountManager.get(context)
            if (oldToken != null) {
                accountManager.invalidateAuthToken(GOOGLE_ACCOUNT_TYPE, oldToken)
            }
            val options = Bundle().apply {
                putString("overridePackage", PACKAGE_NAME_PLAY_STORE)
                putByteArray(
                    "overrideCertificate",
                    Base64.decode(GOOGLE_PLAY_CERT, Base64.DEFAULT)
                )
            }
            accountManager.getAuthToken(
                Account(email, GOOGLE_ACCOUNT_TYPE),
                GOOGLE_PLAY_AUTH_TOKEN_TYPE,
                options,
                notifyAuthFailure,
                { future ->
                    try {
                        continuation.resume(future.result)
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }
                },
                Handler(Looper.getMainLooper())
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch auth token", e)
            continuation.resumeWithException(e)
        }
    }
}
