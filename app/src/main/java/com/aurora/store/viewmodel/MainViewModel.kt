package com.aurora.store.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.Constants
import com.aurora.store.BuildConfig
import com.aurora.store.data.model.SelfUpdate
import com.aurora.store.data.network.HttpClient
import com.aurora.store.util.CertUtil
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.lang.reflect.Modifier

class MainViewModel : ViewModel() {

    private val TAG = MainViewModel::class.java.simpleName

    private val _selfUpdateAvailable = MutableSharedFlow<SelfUpdate?>()
    val selfUpdateAvailable = _selfUpdateAvailable.asSharedFlow()

    fun checkSelfUpdate(context: Context) {
        Log.i(TAG, "Checking for Aurora Store updates")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val gson: Gson =
                    GsonBuilder().excludeFieldsWithModifiers(Modifier.TRANSIENT).create()
                val response = HttpClient.getPreferredClient(context).get(Constants.UPDATE_URL, mapOf())
                val selfUpdate =
                    gson.fromJson(String(response.responseBytes), SelfUpdate::class.java)

                if (selfUpdate.versionCode > BuildConfig.VERSION_CODE) {
                    if (CertUtil.isFDroidApp(context, BuildConfig.APPLICATION_ID)) {
                        if (selfUpdate.fdroidBuild.isNotEmpty()) {
                            _selfUpdateAvailable.emit(selfUpdate)
                        }
                    } else if (selfUpdate.auroraBuild.isNotEmpty()) {
                        _selfUpdateAvailable.emit(selfUpdate)
                    } else {
                        _selfUpdateAvailable.emit(null)
                    }
                } else {
                    _selfUpdateAvailable.emit(null)
                }
            } catch (exception: Exception) {
                _selfUpdateAvailable.emit(null)
                Log.d(TAG, "Failed to check Aurora Store updates", exception)
            }
        }
    }
}
