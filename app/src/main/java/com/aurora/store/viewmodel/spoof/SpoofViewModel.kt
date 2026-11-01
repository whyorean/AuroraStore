/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.viewmodel.spoof

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import com.aurora.extensions.TAG
import com.aurora.extensions.toast
import com.aurora.extensions.withPlayRegion
import com.aurora.gplayapi.android.NativeDeviceProperties
import com.aurora.gplayapi.device.DeviceRegistry
import com.aurora.store.R
import com.aurora.store.data.providers.SpoofProvider
import com.aurora.store.util.PathUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.Properties
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class SpoofViewModel @Inject constructor(
    private val spoofProvider: SpoofProvider,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val defaultLocale: Locale = Locale.getDefault().withPlayRegion()
    val defaultProperties = spoofProvider.nativeDeviceProperties

    private val _currentLocale = MutableStateFlow(spoofProvider.locale)
    val currentLocale = _currentLocale.asStateFlow()

    private val _availableLocales = MutableStateFlow(spoofProvider.availableSpoofLocales)
    val availableLocales = _availableLocales.asStateFlow()

    private val _currentDevice = MutableStateFlow(spoofProvider.deviceProperties)
    val currentDevice = _currentDevice.asStateFlow()

    private val _availableDevices = MutableStateFlow(spoofProvider.availableSpoofDeviceProperties)
    val availableDevices = _availableDevices.asStateFlow()

    fun onDeviceSelected(properties: Properties) {
        _currentDevice.value = properties

        if (properties == defaultProperties) {
            spoofProvider.removeSpoofDeviceProperties()
        } else {
            spoofProvider.setSpoofDeviceProperties(properties)
        }
    }

    fun onLocaleSelected(locale: Locale) {
        _currentLocale.value = locale

        if (locale == defaultLocale) {
            spoofProvider.removeSpoofLocale()
        } else {
            spoofProvider.setSpoofLocale(locale)
        }
    }

    /**
     * Copies the picked file into the spoof directory, then validates it before accepting it -
     * v3 imported anything unconditionally, so a config missing fields gplayapi v4 now requires
     * (33 required keys, 11 numeric) would previously sit in the list unnoticed. An invalid
     * config is deleted again rather than left on disk, and the user is told why.
     */
    fun importDeviceSpoof(uri: Uri) {
        val file = PathUtil.getNewEmptySpoofConfig(context)
        try {
            context.contentResolver?.openInputStream(uri)?.use { input ->
                file.outputStream().use { input.copyTo(it) }
            }

            val properties = Properties().apply { file.inputStream().use { load(it) } }
            DeviceRegistry.parse(file.name, properties)

            _availableDevices.value = spoofProvider.availableSpoofDeviceProperties
        } catch (exception: IllegalArgumentException) {
            Log.e(TAG, "Rejected invalid device config", exception)
            file.delete()
            context.toast(R.string.toast_spoof_import_invalid)
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to import device config", exception)
            file.delete()
        }
    }

    fun exportDeviceSpoof(uri: Uri) {
        try {
            NativeDeviceProperties.read(context)
                .store(context.contentResolver?.openOutputStream(uri), "DEVICE_CONFIG")
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to export device config", exception)
        }
    }
}
