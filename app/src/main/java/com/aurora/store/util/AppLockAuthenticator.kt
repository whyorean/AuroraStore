/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.util

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.fragment.app.FragmentActivity

/**
 * Thin wrapper over [BiometricPrompt] that authenticates with the device biometric *or* the
 * device credential (PIN / pattern / password). Devices without a fingerprint sensor — TVs,
 * older phones — therefore fall back to the lockscreen credential automatically.
 *
 * Combining a biometric with [DEVICE_CREDENTIAL] is only supported by the prompt on API 30+;
 * on older releases the deprecated `setDeviceCredentialAllowed` provides the same behaviour.
 */
object AppLockAuthenticator {

    private val supportsCombinedAuthenticators = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    /**
     * Whether the device can satisfy an app-lock challenge. True when a biometric or device
     * credential is enrolled, or — for the pre-API-30 fallback path — the device simply has a
     * secure lockscreen set.
     */
    fun canAuthenticate(context: Context): Boolean {
        val authenticators = if (supportsCombinedAuthenticators) {
            BIOMETRIC_STRONG or DEVICE_CREDENTIAL
        } else {
            BIOMETRIC_STRONG
        }
        if (BiometricManager.from(context).canAuthenticate(authenticators) ==
            BiometricManager.BIOMETRIC_SUCCESS
        ) {
            return true
        }

        // Pre-API-30 path: no enrolled biometric, but a PIN/pattern/password works as fallback
        return context.getSystemService<KeyguardManager>()?.isDeviceSecure == true
    }

    /**
     * Shows the authentication prompt. [onSuccess] fires on a successful unlock; [onError]
     * fires on a non-recoverable error or user cancellation (the prompt itself handles
     * transient failures like a wrong fingerprint).
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (CharSequence) -> Unit
    ) {
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) =
                onSuccess()

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) =
                onError(errString)
        }

        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            callback
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .apply {
                if (supportsCombinedAuthenticators) {
                    setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
                } else {
                    @Suppress("DEPRECATION")
                    setDeviceCredentialAllowed(true)
                }
            }
            .build()

        prompt.authenticate(promptInfo)
    }
}
