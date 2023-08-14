package com.aurora.store.viewmodel.spoof

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.store.data.providers.SpoofDeviceProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Properties

class SpoofViewModel: ViewModel() {

    private val _availableDevices: MutableStateFlow<List<Properties>> = MutableStateFlow(emptyList())
    val availableDevices = _availableDevices.asStateFlow()

    fun fetchAvailableDevices(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _availableDevices.value = SpoofDeviceProvider.with(context).availableDevice
        }
    }
}
