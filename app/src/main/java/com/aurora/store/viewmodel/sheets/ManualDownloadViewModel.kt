package com.aurora.store.viewmodel.sheets

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.helpers.PurchaseHelper
import com.aurora.store.AuroraApp
import com.aurora.store.data.event.BusEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManualDownloadViewModel @Inject constructor(
    private val purchaseHelper: PurchaseHelper
) : ViewModel() {

    private val TAG = ManualDownloadViewModel::class.java.simpleName

    private val _purchaseStatus = MutableSharedFlow<Boolean>()
    val purchaseStatus = _purchaseStatus.asSharedFlow()

    fun purchase(app: App, customVersion: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val files = purchaseHelper.purchase(app.packageName, customVersion, app.offerType)
                if (files.isNotEmpty()) {
                    AuroraApp.events.send(
                        BusEvent.ManualDownload(app.packageName, customVersion)
                    )
                }
                _purchaseStatus.emit(files.isNotEmpty())
            } catch (exception: Exception) {
                _purchaseStatus.emit(false)
                Log.e(TAG, "Failed to find version: $customVersion", exception)
            }
        }
    }
}
