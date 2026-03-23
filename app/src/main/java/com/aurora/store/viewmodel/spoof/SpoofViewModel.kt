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

    private var currentDevice = spoofProvider.deviceProperties.getProperty("UserReadableName")
    private var currentLocale = spoofProvider.locale

    private val _availableLocales: MutableStateFlow<List<Locale>> = MutableStateFlow(
        spoofProvider.availableSpoofLocales
    )
    val availableLocales = _availableLocales.asStateFlow()

    private val _availableDevices: MutableStateFlow<List<Properties>> = MutableStateFlow(
        spoofProvider.availableSpoofDeviceProperties
    )
    val availableDevices = _availableDevices.asStateFlow()

    fun isDeviceSelected(properties: Properties): Boolean {
        return currentDevice == properties.getProperty("UserReadableName")
    }

    fun onDeviceSelected(properties: Properties) {
        currentDevice = properties.getProperty("UserReadableName")

        if (currentDevice == defaultProperties.getProperty("UserReadableName")) {
            spoofProvider.removeSpoofDeviceProperties()
        } else {
            spoofProvider.setSpoofDeviceProperties(properties)
        }
    }

    fun isLocaleSelected(locale: Locale): Boolean {
        return currentLocale == locale
    }

    fun onLocaleSelected(locale: Locale) {
        currentLocale = locale

        if (currentLocale == defaultLocale) {
            spoofProvider.removeSpoofLocale()
        } else {
            spoofProvider.setSpoofLocale(locale)
        }
    }
}
