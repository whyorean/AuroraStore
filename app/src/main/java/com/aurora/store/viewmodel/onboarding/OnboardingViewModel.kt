/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.viewmodel.onboarding

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.aurora.Constants.FLAVOUR_HUAWEI
import com.aurora.Constants.PACKAGE_NAME_GMS
import com.aurora.Constants.PACKAGE_NAME_PLAY_STORE
import com.aurora.extensions.TAG
import com.aurora.store.AuroraApp
import com.aurora.store.BuildConfig
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.data.helper.UpdateHelper
import com.aurora.store.data.providers.BlacklistProvider
import com.aurora.store.data.work.CacheWorker
import com.aurora.store.util.FlavouredUtil
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_INTRO
import com.aurora.store.util.saveDefaultPreferences
import com.aurora.store.util.setupDefaultAutoUpdates
import com.jakewharton.processphoenix.ProcessPhoenix
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class OnboardingUiState(
    val isMicroBundleChecked: Boolean = false,
    val isMicroGBundleInstalled: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    val updateHelper: UpdateHelper,
    val blacklistProvider: BlacklistProvider,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val isMicroGPromptRequired = FlavouredUtil.promptMicroGInstall(context)

    var uiState by mutableStateOf(OnboardingUiState())
        private set

    init {
        AuroraApp.events.installerEvent.onEach {
            when (it) {
                is InstallerEvent.Installed -> confirmBundleInstall()
                else -> {}
            }
        }.launchIn(AuroraApp.scope)
    }

    fun onMicrogTOSChecked(value: Boolean) {
        uiState = uiState.copy(isMicroBundleChecked = value)
    }

    fun finishOnboarding() {
        Log.i(TAG, "Finishing onboarding with defaults")
        context.saveDefaultPreferences()

        if (BuildConfig.FLAVOR == FLAVOUR_HUAWEI) {
            blacklistProvider.blacklist(PACKAGE_NAME_GMS)
            blacklistProvider.blacklist(PACKAGE_NAME_PLAY_STORE)
        }

        context.setupDefaultAutoUpdates(updateHelper)
        CacheWorker.scheduleAutomatedCacheCleanup(context)
        Preferences.putBooleanNow(context, PREFERENCE_INTRO, true)

        // Restart the app to ensure all permissions are granted
        ProcessPhoenix.triggerRebirth(context)
    }

    private fun confirmBundleInstall() {
        if (PackageUtil.isMicroGBundleInstalled(context)) {
            uiState = uiState.copy(isMicroGBundleInstalled = true)
        }
    }
}
