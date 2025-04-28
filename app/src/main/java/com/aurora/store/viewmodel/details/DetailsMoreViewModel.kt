/*
 * SPDX-FileCopyrightText: 2023-2025 The Calyx Institute
 * SPDX-FileCopyrightText: 2024 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.viewmodel.details

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.helpers.AppDetailsHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = DetailsMoreViewModel.Factory::class)
class DetailsMoreViewModel @AssistedInject constructor(
    @Assisted private val dependencies: List<String>,
    private val appDetailsHelper: AppDetailsHelper
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(dependencies: List<String>): DetailsMoreViewModel
    }

    private val TAG = DetailsMoreViewModel::class.java.simpleName

    private val _dependentApps = MutableStateFlow<List<App>?>(emptyList())
    val dependentApps = _dependentApps.asStateFlow()

    init {
        fetchDependencies()
    }

    private fun fetchDependencies() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _dependentApps.value = appDetailsHelper.getAppByPackageName(dependencies)
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch dependencies", exception)
                _dependentApps.value = null
            }
        }
    }
}
