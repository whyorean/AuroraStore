package com.aurora.store.viewmodel.details

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.store.data.providers.AuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class DetailsMoreViewModel : ViewModel() {

    private val TAG = DetailsMoreViewModel::class.java.simpleName

    private val _dependentApps = MutableSharedFlow<List<App>>()
    val dependentApps = _dependentApps.asSharedFlow()

    fun fetchDependentApps(context: Context, app: App) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val authData: AuthData = AuthProvider.with(context).getAuthData()
                _dependentApps.emit(AppDetailsHelper(authData)
                    .getAppByPackageName(app.dependencies.dependentPackages))
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch dependencies", exception)
                _dependentApps.emit(emptyList())
            }
        }
    }
}
