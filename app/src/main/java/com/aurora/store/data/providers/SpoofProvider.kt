/*
 * SPDX-FileCopyrightText: 2021 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.providers

import android.content.Context
import android.util.Log
import androidx.core.app.LocaleManagerCompat
import com.aurora.extensions.TAG
import com.aurora.extensions.isHuawei
import com.aurora.extensions.toast
import com.aurora.extensions.withPlayRegion
import com.aurora.gplayapi.android.NativeDeviceProperties
import com.aurora.gplayapi.device.DeviceProfile
import com.aurora.gplayapi.device.DeviceRegistry
import com.aurora.store.R
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_DOWNLOAD_SYSTEM_LANGUAGES
import com.aurora.store.util.Preferences.PREFERENCE_VENDING_VERSION
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.Properties
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json

/**
 * Provider class to work with device and locale spoofs
 */
@Singleton
class SpoofProvider @Inject constructor(
    private val json: Json,
    @ApplicationContext val context: Context
) : SpoofDeviceProvider(context) {

    companion object {
        private const val LOCALE_SPOOF_ENABLED = "LOCALE_SPOOF_ENABLED"
        private const val LOCALE_SPOOF_LANG = "LOCALE_SPOOF_LANG"
        private const val LOCALE_SPOOF_COUNTRY = "LOCALE_SPOOF_COUNTRY"

        private const val DEVICE_SPOOF_ENABLED = "DEVICE_SPOOF_ENABLED"
        private const val DEVICE_SPOOF_PROPERTIES = "DEVICE_SPOOF_PROPERTIES"

        /**
         * Gated decision 4: the live path always sends these, regardless of what GSF/Vending
         * build is actually installed. [NativeDeviceProperties] reads the real installed
         * versions - right for the device-config export flow, which wants the true values -
         * but Aurora's protocol (Finsky's User-Agent, `PsRhSigner.finskyVersion`, `AuthParams`,
         * `CheckinRequestBuilder`) has only ever been exercised against this exact vintage.
         * Sending the real installed version on the live path would announce a current Play
         * Store build while still speaking a 21.5.17-vintage protocol, a combination no real
         * Aurora install has ever sent.
         */
        private const val PINNED_GSF_VERSION = "203019037"
        private const val PINNED_VENDING_VERSION = "82151710"
        private const val PINNED_VENDING_VERSION_STRING = "21.5.17-21 [0] [PR] 326734551"
    }

    val availableSpoofDeviceProperties get() = availableDeviceProperties
    val availableSpoofLocales = Locale.getAvailableLocales()
        .map { it.withPlayRegion() }
        .distinct()
        .toMutableList()
        .apply {
            remove(Locale.getDefault().withPlayRegion())
            sortBy { it.displayName }
        }

    val deviceProperties: Properties
        get() {
            val currentProperties = if (isDeviceSpoofEnabled) {
                spoofDeviceProperties
            } else {
                nativeDeviceProperties
            }
            setVendingVersion(currentProperties)
            return currentProperties
        }

    /**
     * The running device's real properties: gplayapi-android's [NativeDeviceProperties] reading
     * plus two things that are app policy rather than device reading, so the library
     * deliberately leaves them out - the live-path GSF/Vending version pin (see
     * [PINNED_GSF_VERSION]) and, on Huawei hardware, the Pixel 7a substitution
     * ([stripHuaweiProperties]), which depends on Aurora's own [isHuawei] and chooses which
     * device to present rather than reading the one actually running.
     *
     * Returns a fresh copy of [cachedNativeDeviceProperties] each time, since callers such as
     * [setVendingVersion] write into it.
     */
    val nativeDeviceProperties: Properties
        get() = Properties().apply { putAll(cachedNativeDeviceProperties) }

    /**
     * Read once per process. [deviceProfile] backs every Play call through gplayapi's session
     * supplier, and [NativeDeviceProperties.read] enumerates every EGL config, so reading it
     * per call cost GPU-driver work on each request.
     *
     * Deliberately never refreshed mid-process: gplayapi only uses the signed-in session while
     * its device fingerprint matches this profile, so a profile that moved under a running
     * process (a display-size change, a keyboard plugged in) would silently drop every call to
     * an anonymous session until restart. Frozen, the session keeps matching; the next cold
     * start sees the new profile and AuthProvider re-authenticates against it.
     */
    private val cachedNativeDeviceProperties: Properties by lazy {
        val properties = NativeDeviceProperties.read(context)
        properties.setProperty("GSF.version", PINNED_GSF_VERSION)
        properties.setProperty("Vending.version", PINNED_VENDING_VERSION)
        properties.setProperty("Vending.versionString", PINNED_VENDING_VERSION_STRING)
        if (isHuawei) stripHuaweiProperties(properties) else properties
    }

    val locale: Locale
        get() = if (isLocaleSpoofEnabled) {
            spoofLocale
        } else {
            Locale.getDefault()
        }.withPlayRegion()

    /**
     * The languages to fetch split APKs for beyond [locale], which gplayapi sends as
     * `X-DFE-UserLanguages` - Play answers with one `config.<lang>.apk` per entry.
     *
     * Sourced from the languages the user has actually configured in Android settings
     * rather than a picker of our own: someone who reads English and German has already
     * said so there, and the Play Store itself reads the same list. [locale] is dropped
     * from it, since Play always sends that one and naming it twice would ask for a
     * duplicate split.
     *
     * [LocaleManagerCompat.getSystemLocales], not the app's own
     * `resources.configuration.locales`: that one is resolved against the languages Aurora
     * itself ships translations for, so a user who reads a language Aurora has not been
     * translated into would never be offered its split.
     *
     * Off by default, and a toggle rather than always-on, because every extra language is
     * a bigger download - Google Maps grows about 5 MB across four of them - and that is
     * the user's call to make, not ours.
     */
    val additionalLocales: List<Locale>
        get() {
            if (!Preferences.getBoolean(context, PREFERENCE_DOWNLOAD_SYSTEM_LANGUAGES)) {
                return emptyList()
            }

            val primary = locale.language
            val configured = LocaleManagerCompat.getSystemLocales(context)
            return (0 until configured.size())
                .mapNotNull { configured[it] }
                .filter { it.language.isNotBlank() && it.language != primary }
                .distinctBy { it.language }
        }

    /**
     * [deviceProperties] as a v4 [DeviceProfile] - what `PlayClientModule`'s `PlaySessions`
     * and `AuthProvider` build sessions against.
     *
     * A device config selected before gplayapi v4 validated (33 required keys, 11 numeric)
     * can fail that validation now. Rather than let an [IllegalArgumentException] break every
     * Play call, an invalid *active* spoof is disabled here so this and future calls fall back
     * to the real device, and the user is told once rather than left to wonder why Play
     * requests started failing.
     */
    fun deviceProfile(): DeviceProfile {
        val properties = deviceProperties
        return try {
            DeviceRegistry.parse(properties.getProperty("Build.PRODUCT", "spoofed"), properties)
        } catch (exception: IllegalArgumentException) {
            Log.e(TAG, "Active device spoof is invalid, disabling it", exception)
            if (isDeviceSpoofEnabled) {
                removeSpoofDeviceProperties()
                context.toast(R.string.toast_spoof_device_invalid)
            }
            DeviceRegistry.default
        }
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
            currentProperties.setProperty(
                "Vending.versionString",
                versionStrings[vendingVersionIndex]
            )
        }
    }

    private fun stripHuaweiProperties(properties: Properties): Properties {
        properties["Build.HARDWARE"] = "lynx"
        properties["Build.BOOTLOADER"] = "lynx-1.0-9716681"
        properties["Build.BRAND"] = "google"
        properties["Build.DEVICE"] = "lynx"
        properties["Build.MODEL"] = "Pixel 7a"
        properties["Build.MANUFACTURER"] = "Google"
        properties["Build.PRODUCT"] = "lynx"
        properties["Build.ID"] = "TQ2A.230505.002"
        return properties
    }
}
