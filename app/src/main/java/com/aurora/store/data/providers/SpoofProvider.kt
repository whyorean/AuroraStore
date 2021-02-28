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

package com.aurora.store.data.providers

import android.content.Context
import com.aurora.store.util.Preferences
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.lang.reflect.Modifier
import java.util.*

class SpoofProvider constructor(var context: Context) {

    companion object {
        const val LOCALE_SPOOF_ENABLED = "LOCALE_SPOOF_ENABLED"
        const val LOCALE_SPOOF_LANG = "LOCALE_SPOOF_LANG"
        const val LOCALE_SPOOF_COUNTRY = "LOCALE_SPOOF_COUNTRY"

        const val DEVICE_SPOOF_ENABLED = "DEVICE_SPOOF_ENABLED"
        const val DEVICE_SPOOF_PROPERTIES = "DEVICE_SPOOF_PROPERTIES"
    }

    fun isLocaleSpoofEnabled(): Boolean {
        return Preferences.getBoolean(context, LOCALE_SPOOF_ENABLED)
    }

    fun isDeviceSpoofEnabled(): Boolean {
        return Preferences.getBoolean(context, DEVICE_SPOOF_ENABLED)
    }

    fun getSpoofLocale(): Locale {
        return if (isLocaleSpoofEnabled()) {
            Locale(
                Preferences.getString(context, LOCALE_SPOOF_LANG),
                Preferences.getString(context, LOCALE_SPOOF_COUNTRY)
            )
        } else {
            Locale.getDefault()
        }
    }

    fun getSpoofDeviceProperties(): Properties {
        return if (isDeviceSpoofEnabled()) {
            val gson: Gson =
                GsonBuilder().excludeFieldsWithModifiers(Modifier.STATIC, Modifier.TRANSIENT)
                    .create()
            return gson.fromJson(
                Preferences.getString(context, DEVICE_SPOOF_PROPERTIES),
                Properties::class.java
            )
        } else {
            Properties()
        }
    }

    fun setSpoofLocale(locale: Locale) {
        Preferences.putBoolean(context, LOCALE_SPOOF_ENABLED, true)
        Preferences.putString(context, LOCALE_SPOOF_LANG, locale.language)
        Preferences.putString(context, LOCALE_SPOOF_COUNTRY, locale.country)
    }

    fun setSpoofDeviceProperties(properties: Properties) {
        val gson: Gson =
            GsonBuilder().excludeFieldsWithModifiers(Modifier.STATIC, Modifier.TRANSIENT)
                .create()
        Preferences.putBoolean(context, DEVICE_SPOOF_ENABLED, true)
        Preferences.putString(context, DEVICE_SPOOF_PROPERTIES, gson.toJson(properties))
    }
}