/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.provider.Settings
import android.provider.Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.core.net.toUri
import com.aurora.extensions.isTAndAbove
import com.aurora.extensions.toast
import com.aurora.store.BuildConfig
import com.aurora.store.R
import com.aurora.store.compose.preview.PreviewTemplate
import com.aurora.store.data.model.Permission
import com.aurora.store.data.model.PermissionType
import com.aurora.store.data.providers.PermissionProvider.Companion.isGranted
import com.aurora.store.util.PackageUtil
import kotlin.random.Random

private const val TAG = "PermissionsScreen"

/**
 * Composable to request known set of permissions
 * @param modifier Modifier to apply to the composable
 * @param permissions Set of known permissions to request from the user
 * @param onPermissionCallback Callback when a permission is granted or denied
 */
@Composable
fun PermissionList(
    modifier: Modifier = Modifier,
    permissions: Set<Permission>,
    onPermissionCallback: (type: PermissionType) -> Unit = {}
) {
    val context = LocalContext.current
    var permissionRequested by rememberSaveable { mutableStateOf<PermissionType?>(null) }

    @SuppressLint("InlinedApi", "BatteryLife")
    val intentMap = mapOf(
        PermissionType.STORAGE_MANAGER to PackageUtil.getStorageManagerIntent(context),
        PermissionType.INSTALL_UNKNOWN_APPS to PackageUtil.getInstallUnknownAppsIntent(),
        PermissionType.DOZE_WHITELIST to Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            "package:${BuildConfig.APPLICATION_ID}".toUri()
        ),
        PermissionType.APP_LINKS to Intent(
            ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
            "package:${BuildConfig.APPLICATION_ID}".toUri()
        )
    )

    fun onResult() {
        permissionRequested?.let {
            if (!isGranted(context, it)) context.toast(R.string.permissions_denied)
            onPermissionCallback(it)
        }
    }

    val intentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { onResult() }
    )
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {
            when (permissionRequested) {
                PermissionType.STORAGE_MANAGER -> {
                    intentLauncher.launch(intentMap[PermissionType.STORAGE_MANAGER]!!)
                }

                else -> onResult()
            }
        }
    )

    fun requestPermission(type: PermissionType) {
        try {
            permissionRequested = type
            when (type) {
                PermissionType.EXTERNAL_STORAGE -> {
                    permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }

                PermissionType.POST_NOTIFICATIONS -> {
                    if (isTAndAbove) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                PermissionType.STORAGE_MANAGER -> {
                    if (!isGranted(context, PermissionType.INSTALL_UNKNOWN_APPS)) {
                        context.toast(R.string.toast_permission_installer_required)
                    } else {
                        permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                }

                else -> {
                    val intent = intentMap[type] ?: return
                    intentLauncher.launch(intent)
                }
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Error requesting permission", exception)
            permissionRequested = null
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_xxsmall))
    ) {
        permissions.sortedBy { it.optional }
            .groupBy { permission -> permission.optional }
            .forEach { (key, value) ->
                stickyHeader {
                    TextDividerComposable(
                        title = if (!key) {
                            stringResource(R.string.item_required)
                        } else {
                            stringResource(R.string.item_optional)
                        }
                    )
                }

                items(items = value, key = { p -> p.type.name }) { permission ->
                    PermissionListItem(
                        permission = permission,
                        onAction = { requestPermission(permission.type) }
                    )
                }
            }
    }
}

@Preview(showBackground = true)
@Composable
private fun PermissionListPreview() {
    val permissions = PermissionType.entries.map { type ->
        Permission(
            type = type,
            title = LoremIpsum(3).values.first(),
            subtitle = LoremIpsum(7).values.first(),
            optional = Random.nextBoolean(),
            isGranted = Random.nextBoolean()
        )
    }.toSet()
    PreviewTemplate {
        PermissionList(permissions = permissions)
    }
}
