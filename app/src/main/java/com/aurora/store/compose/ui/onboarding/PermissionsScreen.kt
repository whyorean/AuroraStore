/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.onboarding

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.provider.Settings
import android.provider.Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurora.extensions.adaptiveNavigationIcon
import com.aurora.extensions.isTAndAbove
import com.aurora.extensions.toast
import com.aurora.store.BuildConfig
import com.aurora.store.R
import com.aurora.store.compose.composables.PermissionComposable
import com.aurora.store.compose.composables.TextDividerComposable
import com.aurora.store.compose.composables.TopAppBarComposable
import com.aurora.store.data.model.Permission
import com.aurora.store.data.model.PermissionType
import com.aurora.store.data.providers.PermissionProvider.Companion.isGranted
import com.aurora.store.util.PackageUtil
import com.aurora.store.viewmodel.onboarding.PermissionsViewModel
import kotlin.random.Random

private const val TAG = "PermissionsScreen"

@Composable
fun PermissionsScreen(
    isOnboarding: Boolean = false,
    onNavigateUp: () -> Unit,
    onPermissionCallback: (type: PermissionType) -> Unit = {},
    viewModel: PermissionsViewModel = hiltViewModel()
) {
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()

    ScreenContent(
        isOnboarding = isOnboarding,
        permissions = permissions,
        onNavigateUp = onNavigateUp,
        onPermissionCallback = { type ->
            viewModel.refreshPermissionsList()
            onPermissionCallback(type)
        }
    )
}

@Composable
private fun ScreenContent(
    isOnboarding: Boolean = false,
    permissions: List<Permission> = emptyList(),
    onNavigateUp: () -> Unit = {},
    onPermissionCallback: (type: PermissionType) -> Unit = {},
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo()
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

    val intentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { permissionRequested?.let { onPermissionCallback(it) } }
    )
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {
            when (permissionRequested) {
                PermissionType.STORAGE_MANAGER -> {
                    intentLauncher.launch(intentMap[PermissionType.STORAGE_MANAGER]!!)
                }

                else -> permissionRequested?.let { onPermissionCallback(it) }
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

    Scaffold(
        topBar = {
            TopAppBarComposable(
                title = stringResource(R.string.onboarding_title_permissions),
                navigationIcon = windowAdaptiveInfo.adaptiveNavigationIcon,
                onNavigateUp = if (isOnboarding) null else onNavigateUp
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues),
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
                        PermissionComposable(
                            permission = permission,
                            onAction = { requestPermission(permission.type) }
                        )
                    }
                }
        }
    }
}

@Preview
@Composable
private fun PermissionsScreenPreview() {
    val permissions = PermissionType.entries.map { type ->
        Permission(
            type = type,
            title = LoremIpsum(3).values.first(),
            subtitle = LoremIpsum(7).values.first(),
            optional = Random.nextBoolean(),
            isGranted = Random.nextBoolean()
        )
    }
    ScreenContent(permissions = permissions)
}
