/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.viewmodel.details

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel(assistedFactory = DetailsPermissionViewModel.Factory::class)
class DetailsPermissionViewModel @AssistedInject constructor(
    @Assisted private val permissions: List<String>,
    @ApplicationContext private val context: Context
): ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(permissions: List<String>): DetailsPermissionViewModel
    }

    private val _permissionsInfo = MutableStateFlow<Map<String, PermissionInfo?>>(emptyMap())
    val permissionsInfo = _permissionsInfo.asStateFlow()

    init {
        fetchPermissions()
    }

    private fun fetchPermissions() {
        _permissionsInfo.value = permissions.associateWith { getPermissionInfo(it) }
    }

    private fun getPermissionInfo(permissionName: String): PermissionInfo? {
        return try {
            context.packageManager.getPermissionInfo(permissionName, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }
}
