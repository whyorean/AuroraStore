/*
 * Aurora Store
 *  Copyright (C) 2021, Rahul Kumar Patel <whyorean@gmail.com>
 *
 *  Aurora Store is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Aurora Store is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.aurora.store.util

import android.content.Context
import android.content.SharedPreferences
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager

object Preferences {

    const val PREFERENCE_DEFAULT = "PREFERENCE_DEFAULT"

    const val PREFERENCE_AUTH_DATA = "PREFERENCE_AUTH_DATA"
    const val PREFERENCE_INSTALLER_ID = "PREFERENCE_INSTALLER_ID"
    const val PREFERENCE_THEME_TYPE = "PREFERENCE_THEME_TYPE"
    const val PREFERENCE_THEME_ACCENT = "PREFERENCE_THEME_ACCENT"
    const val PREFERENCE_INTRO = "PREFERENCE_INTRO"

    const val PREFERENCE_FILTER_GOOGLE = "PREFERENCE_FILTER_GOOGLE"
    const val PREFERENCE_FILTER_FDROID = "PREFERENCE_FILTER_FDROID"

    const val PREFERENCE_AUTO_INSTALL = "PREFERENCE_AUTO_INSTALL"
    const val PREFERENCE_AUTO_DELETE = "PREFERENCE_AUTO_DELETE"

    const val INSTALLATION_ABANDON_SESSION = "INSTALLATION_ABANDON_SESSION"

    const val PREFERENCE_DOWNLOAD_ACTIVE = "PREFERENCE_DOWNLOAD_ACTIVE"
    const val PREFERENCE_DOWNLOAD_EXTERNAL = "PREFERENCE_DOWNLOAD_EXTERNAL"
    const val PREFERENCE_DOWNLOAD_WIFI = "PREFERENCE_DOWNLOAD_WIFI"

    const val PREFERENCE_TOS_READ = "PREFERENCE_TOS_READ"


    private fun getPrefs(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    fun putString(context: Context, key: String, value: String) {
        getPrefs(context).edit().putString(key, value).apply()
    }

    fun putInteger(context: Context, key: String, value: Int) {
        getPrefs(context).edit().putInt(key, value).apply()
    }

    fun putFloat(context: Context, key: String, value: Float) {
        getPrefs(context).edit().putFloat(key, value).apply()
    }

    fun putLong(context: Context, key: String, value: Long) {
        getPrefs(context).edit().putLong(key, value).apply()
    }

    fun putBoolean(context: Context, key: String, value: Boolean) {
        getPrefs(context).edit().putBoolean(key, value).apply()
    }

    fun getString(context: Context, key: String): String {
        return getPrefs(context).getString(key, "").toString()
    }

    fun getInteger(context: Context, key: String): Int {
        return getPrefs(context).getInt(key, 0)
    }

    fun getFloat(context: Context, key: String): Float {
        return getPrefs(context).getFloat(key, 0.0f)
    }

    fun getLong(context: Context, key: String): Long {
        return getPrefs(context).getLong(key, 0L)
    }

    fun getBoolean(context: Context, key: String): Boolean {
        return getPrefs(context).getBoolean(key, false)
    }
}

/*Preference Extensions*/

fun Context.save(key: String, value: Int) = Preferences.putInteger(this, key, value)

fun Context.save(key: String, value: Boolean) = Preferences.putBoolean(this, key, value)

fun Context.save(key: String, value: String) = Preferences.putString(this, key, value)


fun Fragment.save(key: String, value: Int) = requireContext().save(key, value)

fun Fragment.save(key: String, value: Boolean) = requireContext().save(key, value)

fun Fragment.save(key: String, value: String) = requireContext().save(key, value)
