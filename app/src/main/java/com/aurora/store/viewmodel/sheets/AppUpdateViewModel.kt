/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.viewmodel.sheets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.store.data.helper.UpdateHelper
import com.aurora.store.data.providers.BlacklistProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class AppUpdateViewModel @Inject constructor(
    val blacklistProvider: BlacklistProvider,
    private val updateHelper: UpdateHelper
) : ViewModel() {

    fun ignoreAllUpdates(packageName: String) {
        viewModelScope.launch { updateHelper.ignoreAll(packageName) }
    }

    fun ignoreVersion(packageName: String, versionCode: Long) {
        viewModelScope.launch { updateHelper.ignoreVersion(packageName, versionCode) }
    }
}
