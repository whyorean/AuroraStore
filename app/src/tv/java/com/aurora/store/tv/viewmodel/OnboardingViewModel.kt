/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.tv.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.aurora.store.data.helper.UpdateHelper
import com.aurora.store.data.work.CacheWorker
import com.aurora.store.util.Preferences
import com.aurora.store.util.saveDefaultPreferences
import com.aurora.store.util.setupDefaultAutoUpdates
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val updateHelper: UpdateHelper,
    @ApplicationContext private val context: Context
) : ViewModel() {

    /**
     * Completes TV first-run setup, mirroring the phone onboarding's defaults: seed the shared
     * first-run preferences (dispenser URLs, filters, installer, …), schedule background
     * auto-updates and periodic cache cleanup, and mark the intro as completed. Unlike phone
     * onboarding there is no ProcessPhoenix restart — the prefs are read after this in the normal
     * flow.
     */
    fun finishOnboarding() {
        context.saveDefaultPreferences()
        context.setupDefaultAutoUpdates(updateHelper)
        CacheWorker.scheduleAutomatedCacheCleanup(context)
        Preferences.putBooleanNow(context, Preferences.PREFERENCE_INTRO, true)
    }
}
