/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.util

import android.content.Context
import com.aurora.extensions.areNotificationsEnabled
import com.aurora.extensions.isIgnoringBatteryOptimizations
import com.aurora.store.data.helper.UpdateHelper
import com.aurora.store.data.model.UpdateMode

/**
 * Seeds the first-run background auto-update defaults and schedules the periodic check. Shared by
 * the onboarding flow (phone/tablet) and the TV variant (which has no onboarding) so the two entry
 * points cannot drift. The chosen [UpdateMode] is the most capable mode the device currently
 * allows: install silently when battery-unrestricted, else notify when notifications are enabled,
 * else disabled.
 */
fun Context.setupDefaultAutoUpdates(updateHelper: UpdateHelper) {
    val updateMode = when {
        isIgnoringBatteryOptimizations() -> UpdateMode.CHECK_AND_INSTALL
        areNotificationsEnabled() -> UpdateMode.CHECK_AND_NOTIFY
        else -> UpdateMode.DISABLED
    }

    save(Preferences.PREFERENCE_UPDATES_AUTO, updateMode.ordinal)
    save(Preferences.PREFERENCE_UPDATES_CHECK_INTERVAL, 3)
    updateHelper.scheduleAutomatedCheck()
}
