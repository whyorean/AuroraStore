/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.viewmodel.details

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import androidx.lifecycle.ViewModel
import com.aurora.store.util.PackageUtil
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel(assistedFactory = PermissionViewModel.Factory::class)
class PermissionViewModel @AssistedInject constructor(
    @Assisted private val packageName: String,
    @Assisted private val permissions: List<String>,
    @ApplicationContext private val context: Context
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(packageName: String, permissions: List<String>): PermissionViewModel
    }

    private val _permissionsInfo = MutableStateFlow<Map<String, PermissionInfo>>(emptyMap())
    val permissionsInfo = _permissionsInfo.asStateFlow()

    private val _newPermissions = MutableStateFlow<Set<String>>(emptySet())
    val newPermissions = _newPermissions.asStateFlow()

    init {
        fetchPermissions()
    }

    private fun fetchPermissions() {
        // Bail out if this is not a known permission for the OS
        _permissionsInfo.value = permissions.mapNotNull { permission ->
            getPermissionInfo(permission)?.let { info -> permission to info }
        }.toMap()

        val installed = try {
            PackageUtil.getPackageInfo(context, packageName, PackageManager.GET_PERMISSIONS)
                .requestedPermissions
                ?.toSet()
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
        if (installed != null) {
            _newPermissions.value = _permissionsInfo.value.keys - installed
        }
    }

    private fun getPermissionInfo(permissionName: String): PermissionInfo? = try {
        context.packageManager.getPermissionInfo(permissionName, 0)
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }
}
