package com.aurora.store.view.ui.preferences

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.Constants
import com.aurora.store.data.model.ProxyInfo
import com.aurora.store.data.network.HttpClient
import com.aurora.store.util.Preferences.PREFERENCE_PROXY_INFO
import com.aurora.store.util.Preferences.PREFERENCE_PROXY_URL
import com.aurora.store.util.save
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak") // false positive, see https://github.com/google/dagger/issues/3253
class SettingsViewModel @Inject constructor(
    private val gson: Gson,
    @ApplicationContext private val context: Context
): ViewModel() {

    private val TAG = SettingsViewModel::class.java.simpleName

    private val _proxyURL = MutableStateFlow(String())
    val proxyURL = _proxyURL.asStateFlow()

    private val _proxyInfo = MutableSharedFlow<ProxyInfo?>()
    val proxyInfo = _proxyInfo.asSharedFlow()

    fun saveProxyDetails(url: String, info: ProxyInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = HttpClient
                    .getPreferredClient(context)
                    .setProxy(info)
                    .get(Constants.ANDROID_CONNECTIVITY_URL, mapOf())

                if (result.code == 204) {
                    context.save(PREFERENCE_PROXY_URL, url)
                    _proxyURL.value = url

                    context.save(PREFERENCE_PROXY_INFO, gson.toJson(info))
                    _proxyInfo.emit(info)
                } else {
                    throw Exception("Failed to set proxy")
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to set proxy", exception)
                _proxyInfo.emit(null)
            }
        }
    }
}
