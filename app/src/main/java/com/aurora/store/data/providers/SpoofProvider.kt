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
import com.aurora.store.R
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_VENDING_VERSION
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import java.util.Locale
import java.util.Properties
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provider class to work with device and locale spoofs
 */
@Singleton
class SpoofProvider @Inject constructor(
    private val json: Json,
    @ApplicationContext val context: Context,
) : SpoofDeviceProvider(context) {

    companion object {
        private const val LOCALE_SPOOF_ENABLED = "LOCALE_SPOOF_ENABLED"
        private const val LOCALE_SPOOF_LANG = "LOCALE_SPOOF_LANG"
        private const val LOCALE_SPOOF_COUNTRY = "LOCALE_SPOOF_COUNTRY"

        private const val DEVICE_SPOOF_ENABLED = "DEVICE_SPOOF_ENABLED"
        private const val DEVICE_SPOOF_PROPERTIES = "DEVICE_SPOOF_PROPERTIES"
    }

    val availableSpoofDeviceProperties get() = availableDeviceProperties
    val availableSpoofLocales = Locale.getAvailableLocales().toMutableList().apply {
        remove(Locale.getDefault())
        sortBy { it.displayName }
    }

    val deviceProperties: Properties
        get() {
            val currentProperties = if (isDeviceSpoofEnabled) {
                spoofDeviceProperties
            } else {
                NativeDeviceInfoProvider.getNativeDeviceProperties(context)
            }
            setVendingVersion(currentProperties)
            return currentProperties
        }

    val locale: Locale
        get() = if (isLocaleSpoofEnabled) {
            spoofLocale
        } else {
            Locale.getDefault()
        }

    val isLocaleSpoofEnabled: Boolean
        get() = Preferences.getBoolean(context, LOCALE_SPOOF_ENABLED)

    val isDeviceSpoofEnabled: Boolean
        get() = Preferences.getBoolean(context, DEVICE_SPOOF_ENABLED)

    private val spoofLocale: Locale
        get() = Locale.Builder()
            .setLanguage(Preferences.getString(context, LOCALE_SPOOF_LANG))
            .setRegion(Preferences.getString(context, LOCALE_SPOOF_COUNTRY))
            .build()

    private val spoofDeviceProperties: Properties
        get() = json.decodeFromString<Properties>(
            Preferences.getString(context, DEVICE_SPOOF_PROPERTIES)
        )

    fun setSpoofLocale(locale: Locale) {
        Preferences.putBoolean(context, LOCALE_SPOOF_ENABLED, true)
        Preferences.putString(context, LOCALE_SPOOF_LANG, locale.language)
        Preferences.putString(context, LOCALE_SPOOF_COUNTRY, locale.country)
    }

    fun setSpoofDeviceProperties(properties: Properties) {
        Preferences.putBoolean(context, DEVICE_SPOOF_ENABLED, true)
        Preferences.putString(context, DEVICE_SPOOF_PROPERTIES, json.encodeToString(properties))
    }

    fun removeSpoofLocale() {
        Preferences.remove(context, LOCALE_SPOOF_ENABLED)
        Preferences.remove(context, LOCALE_SPOOF_LANG)
        Preferences.remove(context, LOCALE_SPOOF_COUNTRY)
    }

    fun removeSpoofDeviceProperties() {
        Preferences.remove(context, DEVICE_SPOOF_ENABLED)
        Preferences.remove(context, DEVICE_SPOOF_PROPERTIES)
    }

    private fun setVendingVersion(currentProperties: Properties) {
        val vendingVersionIndex = Preferences.getInteger(context, PREFERENCE_VENDING_VERSION)
        if (vendingVersionIndex > 0) {
            val resources = context.resources
            val versionCodes = resources.getStringArray(R.array.pref_vending_version_codes)
            val versionStrings = resources.getStringArray(R.array.pref_vending_version)

            currentProperties.setProperty("Vending.version", versionCodes[vendingVersionIndex])
            currentProperties.setProperty("Vending.versionString", versionStrings[vendingVersionIndex])
        }
    }
}
