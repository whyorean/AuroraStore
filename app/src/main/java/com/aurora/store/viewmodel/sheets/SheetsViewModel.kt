package com.aurora.store.viewmodel.sheets

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.helpers.PurchaseHelper
import com.aurora.store.data.event.BusEvent
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.data.work.ExportWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

@HiltViewModel
class SheetsViewModel @Inject constructor(
    private val authProvider: AuthProvider
) : ViewModel() {

    private val TAG = SheetsViewModel::class.java.simpleName

    private val _purchaseStatus = MutableSharedFlow<Boolean>()
    val purchaseStatus = _purchaseStatus.asSharedFlow()

    fun purchase(app: App, customVersion: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val purchaseHelper = PurchaseHelper(authProvider.authData)
                val files = purchaseHelper.purchase(app.packageName, customVersion, app.offerType)
                if (files.isNotEmpty()) {
                    EventBus.getDefault()
                        .post(BusEvent.ManualDownload(app.packageName, customVersion))
                }
                _purchaseStatus.emit(files.isNotEmpty())
            } catch (exception: Exception) {
                _purchaseStatus.emit(false)
                Log.e(TAG, "Failed to find version: $customVersion", exception)
            }
        }
    }

    fun copyInstalledApp(context: Context, packageName: String, uri: Uri) {
        ExportWorker.exportInstalledApp(context, packageName, uri)
    }

    fun copyDownloadedApp(context: Context, packageName: String, versionCode: Int, uri: Uri) {
        ExportWorker.exportDownloadedApp(context, packageName, versionCode, uri)
    }
}
