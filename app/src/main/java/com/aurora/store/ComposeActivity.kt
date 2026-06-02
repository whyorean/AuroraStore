/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.IntentCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurora.extensions.getPackageName
import com.aurora.store.R
import com.aurora.store.compose.composition.LocalNetworkStatus
import com.aurora.store.compose.composition.LocalUI
import com.aurora.store.compose.composition.UI
import com.aurora.store.compose.navigation.NavDisplay
import com.aurora.store.compose.navigation.Screen
import com.aurora.store.compose.theme.AuroraTheme
import com.aurora.store.compose.ui.lock.AppLockScreen
import com.aurora.store.data.AppLockManager
import com.aurora.store.data.model.NetworkStatus
import com.aurora.store.data.providers.NetworkProvider
import com.aurora.store.data.receiver.MigrationReceiver
import com.aurora.store.util.AppLockAuthenticator
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.Preferences
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ComposeActivity : FragmentActivity() {

    @Inject lateinit var networkProvider: NetworkProvider

    @Inject lateinit var appLockManager: AppLockManager

    override fun onCreate(savedInstanceState: Bundle?) {
        MigrationReceiver.runMigrationsIfRequired(this)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        intent.setExtrasClassLoader(Screen::class.java.classLoader)

        val startDestination = resolveStartDestination()

        val localUI = when {
            PackageUtil.isTv(this) -> UI.TV
            else -> UI.DEFAULT
        }

        setContent {
            val networkStatus by networkProvider.status.collectAsStateWithLifecycle(
                initialValue = NetworkStatus.AVAILABLE
            )
            AuroraTheme {
                var lockState by remember {
                    mutableStateOf(
                        if (appLockManager.shouldLock(this@ComposeActivity)) {
                            LockState.AUTHENTICATING
                        } else {
                            LockState.UNLOCKED
                        }
                    )
                }

                // Keep FLAG_SECURE on until unlocked; auto-prompt while authenticating
                LaunchedEffect(lockState) {
                    when (lockState) {
                        LockState.AUTHENTICATING -> {
                            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                            promptUnlock(
                                onSuccess = { lockState = LockState.UNLOCKED },
                                onError = { lockState = LockState.LOCKED }
                            )
                        }

                        LockState.LOCKED -> window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                        LockState.UNLOCKED ->
                            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }

                // Re-lock when returning from the background past the grace timeout
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_STOP -> appLockManager.onBackgrounded()
                            Lifecycle.Event.ON_START ->
                                if (lockState == LockState.UNLOCKED &&
                                    appLockManager.shouldLock(this@ComposeActivity)
                                ) {
                                    lockState = LockState.AUTHENTICATING
                                }

                            else -> {}
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                when (lockState) {
                    LockState.UNLOCKED -> CompositionLocalProvider(
                        LocalUI provides localUI,
                        LocalNetworkStatus provides networkStatus
                    ) {
                        NavDisplay(startDestination = startDestination)
                    }

                    // Plain surface behind the prompt so dismissing it doesn't flash the lock card
                    LockState.AUTHENTICATING -> Surface(modifier = Modifier.fillMaxSize()) {}

                    LockState.LOCKED -> AppLockScreen(
                        onUnlock = { lockState = LockState.AUTHENTICATING }
                    )
                }
            }
        }
    }

    private fun promptUnlock(onSuccess: () -> Unit, onError: () -> Unit) {
        AppLockAuthenticator.authenticate(
            activity = this,
            title = getString(R.string.app_lock_prompt_title),
            subtitle = getString(R.string.app_lock_prompt_subtitle),
            onSuccess = {
                appLockManager.markUnlocked()
                onSuccess()
            },
            onError = { onError() }
        )
    }

    private fun resolveStartDestination(): Screen {
        // Parcel-based navigation (e.g. from NotificationUtil or DeepLinkConfirmActivity, which
        // owns the external ACTION_VIEW market:// and play.google.com deep links)
        IntentCompat.getParcelableExtra(intent, Screen.PARCEL_KEY, Screen::class.java)
            ?.let { return it }

        // SEND / SHOW_APP_INFO — getPackageName() handles both
        intent.getPackageName()?.let { return Screen.AppDetails(it) }

        return defaultStart()
    }

    private fun defaultStart(): Screen = when {
        !Preferences.getBoolean(this, Preferences.PREFERENCE_INTRO) -> Screen.Onboarding
        else -> Screen.Splash()
    }

    private enum class LockState { AUTHENTICATING, LOCKED, UNLOCKED }
}
