package com.aurora.store.viewmodel.details

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.store.data.providers.AuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

@HiltViewModel
class DetailsMoreViewModel @Inject constructor(
    private val authProvider: AuthProvider
) : ViewModel() {

    private val TAG = DetailsMoreViewModel::class.java.simpleName

    private val _dependentApps = MutableSharedFlow<List<App>>()
    val dependentApps = _dependentApps.asSharedFlow()

    fun fetchDependentApps(app: App) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _dependentApps.emit(AppDetailsHelper(authProvider.authData)
                    .getAppByPackageName(app.dependencies.dependentPackages))
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch dependencies", exception)
                _dependentApps.emit(emptyList())
            }
        }
    }
}
