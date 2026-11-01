/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.viewmodel.details

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.extensions.isSAndAbove
import com.aurora.store.R
import com.aurora.store.util.PackageUtil
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

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

    data class Group(val label: String, val permissions: List<String>)

    private val _permissionsInfo = MutableStateFlow<Map<String, PermissionInfo>>(emptyMap())
    val permissionsInfo = _permissionsInfo.asStateFlow()

    private val _newPermissions = MutableStateFlow<Set<String>>(emptySet())
    val newPermissions = _newPermissions.asStateFlow()

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups = _groups.asStateFlow()

    private val packageManager = context.packageManager

    init {
        viewModelScope.launch(Dispatchers.IO) { fetchPermissions() }
    }

    private suspend fun fetchPermissions() {
        // Bail out if this is not a known permission for the OS
        val infos = permissions.mapNotNull { permission ->
            getPermissionInfo(permission)?.let { info -> permission to info }
        }.toMap()
        val newPermissions = PackageUtil.getNewPermissions(
            context,
            packageName,
            infos.keys.toList()
        ).toSet()

        val platformGroups = platformGroupsOf(infos.keys)
        _groups.value = infos.keys
            .groupBy { permission ->
                platformGroups[permission]?.let { it to true }
                    ?: fallbackGroupOf(infos.getValue(permission))
            }
            .map { (key, members) ->
                key to Group(key.first, members.sortedByDescending { it in newPermissions })
            }
            .sortedWith(
                compareByDescending<Pair<Pair<String, Boolean>, Group>> { (_, group) ->
                    group.permissions.any { it in newPermissions }
                }
                    .thenByDescending { (key, _) -> key.second }
                    .thenBy { (key, _) -> key.first }
            )
            .map { it.second }
        _newPermissions.value = newPermissions
        _permissionsInfo.value = infos
    }

    // Most platform permissions report an UNDEFINED group since Android 10
    private suspend fun platformGroupsOf(permissions: Set<String>): Map<String, String> {
        if (!isSAndAbove) return emptyMap()
        val mapping = mutableMapOf<String, String>()
        packageManager.getAllPermissionGroups(0).forEach { groupInfo ->
            val members = suspendCancellableCoroutine { continuation ->
                packageManager.getPlatformPermissionsForGroup(
                    groupInfo.name,
                    Dispatchers.IO.asExecutor()
                ) { continuation.resume(it) }
            }
            val label = groupInfo.loadLabel(packageManager).toString()
            members.filter { it in permissions }.forEach { mapping[it] = label }
        }
        return mapping
    }

    private fun fallbackGroupOf(info: PermissionInfo): Pair<String, Boolean> {
        info.group?.takeUnless { it == UNDEFINED_GROUP }?.let { group ->
            runCatching {
                val label = packageManager.getPermissionGroupInfo(group, 0)
                    .loadLabel(packageManager)
                    .toString()
                return label to true
            }
        }
        val bucket = when (info.packageName) {
            "android" -> context.getString(R.string.permission_group_system)
            "com.google.android.gms",
            "com.google.android.gsf",
            "com.android.vending" -> context.getString(R.string.permission_group_google)
            else -> PackageUtil.getPackageLabel(context, info.packageName)
                ?: context.getString(R.string.permission_group_other)
        }
        return bucket to false
    }

    private fun getPermissionInfo(permissionName: String): PermissionInfo? = try {
        packageManager.getPermissionInfo(permissionName, 0)
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    private companion object {
        const val UNDEFINED_GROUP = "android.permission-group.UNDEFINED"
    }
}
