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
import com.aurora.extensions.areNotificationsEnabled
import com.aurora.extensions.isIgnoringBatteryOptimizations
import com.aurora.store.AuroraApp
import com.aurora.store.BuildConfig
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.data.helper.UpdateHelper
import com.aurora.store.data.model.UpdateMode
import com.aurora.store.data.providers.BlacklistProvider
import com.aurora.store.data.work.CacheWorker
import com.aurora.store.util.FlavouredUtil
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_AUTO_DELETE
import com.aurora.store.util.Preferences.PREFERENCE_DEFAULT_SELECTED_TAB
import com.aurora.store.util.Preferences.PREFERENCE_DISPENSER_URLS
import com.aurora.store.util.Preferences.PREFERENCE_FILTER_AURORA_ONLY
import com.aurora.store.util.Preferences.PREFERENCE_FILTER_FDROID
import com.aurora.store.util.Preferences.PREFERENCE_FOR_YOU
import com.aurora.store.util.Preferences.PREFERENCE_INSTALLER_ID
import com.aurora.store.util.Preferences.PREFERENCE_INTRO
import com.aurora.store.util.Preferences.PREFERENCE_THEME_STYLE
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_AUTO
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_CHECK_INTERVAL
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_EXTENDED
import com.aurora.store.util.Preferences.PREFERENCE_VENDING_VERSION
import com.aurora.store.util.save
import com.jakewharton.processphoenix.ProcessPhoenix
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

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
                else                        -> {}
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

        setupAutoUpdates()
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

    private fun setupAutoUpdates() {
        val updateMode = when {
            context.isIgnoringBatteryOptimizations() -> UpdateMode.CHECK_AND_INSTALL
            context.areNotificationsEnabled()        -> UpdateMode.CHECK_AND_NOTIFY
            else                                     -> UpdateMode.DISABLED
        }

        context.save(PREFERENCE_UPDATES_AUTO, updateMode.ordinal)
        context.save(PREFERENCE_UPDATES_CHECK_INTERVAL, 3)
        updateHelper.scheduleAutomatedCheck()
    }

    private fun Context.saveDefaultPreferences() {
        /*Filters*/
        save(PREFERENCE_FILTER_AURORA_ONLY, false)
        save(PREFERENCE_FILTER_FDROID, true)

        /*Network*/
        save(PREFERENCE_DISPENSER_URLS, FlavouredUtil.defaultDispensers)
        save(PREFERENCE_VENDING_VERSION, 0)

        /*Customization*/
        save(PREFERENCE_THEME_STYLE, 0)
        save(PREFERENCE_DEFAULT_SELECTED_TAB, 0)
        save(PREFERENCE_FOR_YOU, true)

        /*Installer*/
        save(PREFERENCE_AUTO_DELETE, true)
        save(PREFERENCE_INSTALLER_ID, 0)

        /*Updates*/
        save(PREFERENCE_UPDATES_EXTENDED, false)
    }
}
