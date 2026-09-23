/*
 * SPDX-FileCopyrightText: 2021 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.aurora.extensions.isOneUI
import com.aurora.store.BuildConfig

object Preferences {

    const val PREFERENCE_DEFAULT = "PREFERENCE_DEFAULT"

    /**
     * Default for [PREFERENCE_DYNAMIC_COLORS]. Dynamic color is opt-out everywhere except One UI,
     * where Samsung's palette extraction tends to look off, so it defaults off and users can opt in.
     */
    val dynamicColorsDefault: Boolean
        get() = !isOneUI

    const val PREFERENCE_AUTH_DATA = "PREFERENCE_AUTH_DATA"
    const val PREFERENCE_INSTALLER_ID = "PREFERENCE_INSTALLER_ID"
    const val PREFERENCE_THEME_STYLE = "PREFERENCE_THEME_STYLE"
    const val PREFERENCE_DYNAMIC_COLORS = "PREFERENCE_DYNAMIC_COLORS"
    const val PREFERENCE_FOR_YOU = "PREFERENCE_FOR_YOU"
    const val PREFERENCE_DEFAULT_SELECTED_TAB = "PREFERENCE_DEFAULT_SELECTED_TAB"
    const val PREFERENCE_INTRO = "PREFERENCE_INTRO"

    const val PREFERENCE_FILTER_FDROID = "PREFERENCE_FILTER_FDROID"
    const val PREFERENCE_FILTER_AURORA_ONLY = "PREFERENCE_FILTER_AURORA_ONLY"
    const val PREFERENCE_FILTER_INSTALLERS = "PREFERENCE_FILTER_INSTALLERS"
    const val PREFERENCE_PLAY_ATTRIBUTED_INSTALLS = "PREFERENCE_PLAY_ATTRIBUTED_INSTALLS"

    const val PREFERENCE_AUTO_DELETE = "PREFERENCE_AUTO_DELETE"

    const val PREFERENCE_NOTIFICATION_PROGRESS = "PREFERENCE_NOTIFICATION_PROGRESS"

    const val PREFERENCE_INSTALLATION_DEVICE_OWNER = "PREFERENCE_INSTALLATION_DEVICE_OWNER"

    const val PREFERENCE_PROXY_URL = "PREFERENCE_PROXY_URL"
    const val PREFERENCE_PROXY_INFO = "PREFERENCE_PROXY_INFO"

    const val PREFERENCE_MICROG_AUTH = "PREFERENCE_MICROG_AUTH"
    const val PREFERENCE_AUTH_VIA_MICROG = "PREFERENCE_AUTH_VIA_MICROG"

    const val PREFERENCE_DISPENSER_URLS = "PREFERENCE_DISPENSER_URLS"
    const val PREFERENCE_VENDING_VERSION = "PREFERENCE_VENDING_VERSION"

    const val PREFERENCE_UPDATES_EXTENDED = "PREFERENCE_UPDATES_EXTENDED"
    const val PREFERENCE_UPDATES_AUTO = "PREFERENCE_UPDATES_AUTO"
    const val PREFERENCE_UPDATES_CHECK_INTERVAL = "PREFERENCE_UPDATES_CHECK_INTERVAL"
    const val PREFERENCE_UPDATES_WARN_TRACKERS = "PREFERENCE_UPDATES_WARN_TRACKERS"
    const val PREFERENCES_UPDATES_RESTRICTIONS = "PREFERENCES_UPDATES_RESTRICTIONS"
    const val PREFERENCES_UPDATES_RESTRICTIONS_METERED = "PREFERENCES_UPDATES_RESTRICTIONS_METERED"
    const val PREFERENCES_UPDATES_RESTRICTIONS_IDLE = "PREFERENCES_UPDATES_RESTRICTIONS_IDLE"
    const val PREFERENCES_UPDATES_RESTRICTIONS_BATTERY = "PREFERENCES_UPDATES_RESTRICTIONS_BATTERY"

    const val PREFERENCE_INSTALLED_SORT_BY = "PREFERENCE_INSTALLED_SORT_BY"
    const val PREFERENCE_INSTALLED_SORT_ORDER = "PREFERENCE_INSTALLED_SORT_ORDER"
    const val PREFERENCE_INSTALLED_APP_TYPES = "PREFERENCE_INSTALLED_APP_TYPES"
    const val PREFERENCE_INSTALLED_INSTALLER = "PREFERENCE_INSTALLED_INSTALLER"

    const val PREFERENCE_BLACKLIST_SORT_BY = "PREFERENCE_BLACKLIST_SORT_BY"
    const val PREFERENCE_BLACKLIST_SORT_ORDER = "PREFERENCE_BLACKLIST_SORT_ORDER"
    const val PREFERENCE_BLACKLIST_APP_TYPES = "PREFERENCE_BLACKLIST_APP_TYPES"
    const val PREFERENCE_BLACKLIST_INSTALLER = "PREFERENCE_BLACKLIST_INSTALLER"

    const val PREFERENCE_DOWNLOADS_SORT_BY = "PREFERENCE_DOWNLOADS_SORT_BY"
    const val PREFERENCE_DOWNLOADS_SORT_ORDER = "PREFERENCE_DOWNLOADS_SORT_ORDER"

    const val PREFERENCE_MIGRATION_VERSION = "PREFERENCE_MIGRATION_VERSION"

    const val PREFERENCE_SELF_UPDATE_ENABLED = "PREFERENCE_SELF_UPDATE_ENABLED"

    const val PREFERENCE_APP_LOCK_ENABLED = "PREFERENCE_APP_LOCK_ENABLED"
    const val PREFERENCE_APP_LOCK_TIMEOUT = "PREFERENCE_APP_LOCK_TIMEOUT"

    const val PREFERENCE_CONFIRM_EXTERNAL_DEEPLINK = "PREFERENCE_CONFIRM_EXTERNAL_DEEPLINK"

    const val PREFERENCE_LAST_TRACKER_SYNC = "PREFERENCE_LAST_TRACKER_SYNC"

    private var prefs: SharedPreferences? = null

    fun getPrefs(context: Context): SharedPreferences = when (BuildConfig.FLAVOR) {
        "vanilla" -> {
            prefs ?: PreferenceManager.getDefaultSharedPreferences(context).also { prefs = it }
        }

        else -> {
            val prefName = "${context.packageName}_${BuildConfig.FLAVOR}_preferences"
            prefs ?: context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
                .also { prefs = it }
        }
    }

    fun remove(context: Context, key: String) {
        getPrefs(context).edit { remove(key) }
    }

    fun putString(context: Context, key: String, value: String) {
        getPrefs(context).edit { putString(key, value) }
    }

    fun putStringSet(context: Context, key: String, value: Set<String>) {
        getPrefs(context).edit { putStringSet(key, value) }
    }

    fun putInteger(context: Context, key: String, value: Int) {
        getPrefs(context).edit { putInt(key, value) }
    }

    fun putFloat(context: Context, key: String, value: Float) {
        getPrefs(context).edit { putFloat(key, value) }
    }

    fun putLong(context: Context, key: String, value: Long) {
        getPrefs(context).edit { putLong(key, value) }
    }

    fun putBoolean(context: Context, key: String, value: Boolean) {
        getPrefs(context).edit { putBoolean(key, value) }
    }

    fun putBooleanNow(context: Context, key: String, value: Boolean) {
        getPrefs(context).edit(true) { putBoolean(key, value) }
    }

    fun getString(context: Context, key: String, default: String = ""): String =
        getPrefs(context).getString(key, default).toString()

    fun getStringSet(
        context: Context,
        key: String,
        default: Set<String> = emptySet()
    ): Set<String> = getPrefs(context).getStringSet(key, default) ?: emptySet()

    fun getInteger(context: Context, key: String, default: Int = 0): Int =
        getPrefs(context).getInt(key, default)

    fun getFloat(context: Context, key: String): Float = getPrefs(context).getFloat(key, 0.0f)

    fun getLong(context: Context, key: String): Long = getPrefs(context).getLong(key, 0L)

    fun getBoolean(context: Context, key: String, default: Boolean = false): Boolean =
        getPrefs(context).getBoolean(key, default)
}

/*Preference Extensions*/

fun Context.save(key: String, value: Int) = Preferences.putInteger(this, key, value)

fun Context.save(key: String, value: Boolean) = Preferences.putBoolean(this, key, value)

fun Context.save(key: String, value: String) = Preferences.putString(this, key, value)

fun Context.save(key: String, value: Set<String>) = Preferences.putStringSet(this, key, value)

fun Context.remove(key: String) = Preferences.remove(this, key)

fun Fragment.save(key: String, value: Int) = requireContext().save(key, value)

fun Fragment.save(key: String, value: Boolean) = requireContext().save(key, value)

fun Fragment.save(key: String, value: String) = requireContext().save(key, value)

fun Fragment.save(key: String, value: Set<String>) = requireContext().save(key, value)

fun Fragment.remove(key: String) = requireContext().remove(key)
