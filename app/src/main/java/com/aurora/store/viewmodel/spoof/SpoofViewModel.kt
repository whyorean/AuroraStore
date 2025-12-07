package com.aurora.store.viewmodel.spoof

import android.content.Context
import androidx.lifecycle.ViewModel
import com.aurora.store.data.providers.NativeDeviceInfoProvider
import com.aurora.store.data.providers.SpoofProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.Properties
import javax.inject.Inject

@HiltViewModel
class SpoofViewModel @Inject constructor(
    val spoofProvider: SpoofProvider,
    @ApplicationContext private val context: Context
): ViewModel() {

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
}
