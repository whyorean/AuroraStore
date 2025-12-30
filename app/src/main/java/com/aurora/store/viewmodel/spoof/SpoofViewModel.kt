package com.aurora.store.viewmodel.spoof

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import com.aurora.extensions.TAG
import com.aurora.store.data.providers.NativeDeviceInfoProvider
import com.aurora.store.data.providers.SpoofProvider
import com.aurora.store.util.PathUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.Properties
import javax.inject.Inject

@HiltViewModel
class SpoofViewModel @Inject constructor(
    private val spoofProvider: SpoofProvider,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val defaultLocale: Locale = Locale.getDefault()
    val defaultProperties = NativeDeviceInfoProvider.getNativeDeviceProperties(context)

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

        if (currentDevice == defaultProperties) {
            spoofProvider.removeSpoofDeviceProperties()
        } else {
            spoofProvider.setSpoofDeviceProperties(properties)
        }
    }

    fun onLocaleSelected(locale: Locale) {
        _currentLocale.value = locale

        if (currentLocale == defaultLocale) {
            spoofProvider.removeSpoofLocale()
        } else {
            spoofProvider.setSpoofLocale(locale)
        }
    }

    fun importDeviceSpoof(uri: Uri) {
        try {
            context.contentResolver?.openInputStream(uri)?.use { input ->
                PathUtil.getNewEmptySpoofConfig(context).outputStream().use {
                    input.copyTo(it)
                }
            }
            _availableDevices.value = spoofProvider.availableSpoofDeviceProperties
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to import device config", exception)
        }
    }

    fun exportDeviceSpoof(uri: Uri) {
        try {
            NativeDeviceInfoProvider.getNativeDeviceProperties(context, true)
                .store(context.contentResolver?.openOutputStream(uri), "DEVICE_CONFIG")
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to export device config", exception)
        }
    }
}
