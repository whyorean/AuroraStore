package com.aurora.store.viewmodel.spoof

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.store.data.providers.SpoofDeviceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Properties
import javax.inject.Inject

@HiltViewModel
class SpoofViewModel @Inject constructor(
    private val spoofDeviceProvider: SpoofDeviceProvider
): ViewModel() {

    private val _availableDevices: MutableStateFlow<List<Properties>> = MutableStateFlow(emptyList())
    val availableDevices = _availableDevices.asStateFlow()

    fun fetchAvailableDevices() {
        viewModelScope.launch(Dispatchers.IO) {
            _availableDevices.value = spoofDeviceProvider.availableDevice
        }
    }
}
