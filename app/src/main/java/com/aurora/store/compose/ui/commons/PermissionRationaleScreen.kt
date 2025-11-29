/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.commons

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurora.extensions.adaptiveNavigationIcon
import com.aurora.store.R
import com.aurora.store.compose.composable.PermissionList
import com.aurora.store.compose.composable.TopAppBar
import com.aurora.store.compose.preview.PreviewTemplate
import com.aurora.store.data.model.Permission
import com.aurora.store.data.model.PermissionType
import com.aurora.store.viewmodel.commons.PermissionRationaleViewModel
import kotlin.random.Random

/**
 * Screen to request specific set of permissions expected to be used during installing an app
 */
@Composable
fun PermissionRationaleScreen(
    requiredPermissions: Set<PermissionType> = emptySet(),
    onNavigateUp: () -> Unit,
    onPermissionCallback: (type: PermissionType) -> Unit = {},
    viewModel: PermissionRationaleViewModel = hiltViewModel()
) {
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()

    ScreenContent(
        onNavigateUp = onNavigateUp,
        permissions = permissions
            .filter { it.type in requiredPermissions }
            .map { permission -> permission.copy(optional = false) }
            .toSet(),
        onPermissionCallback = { type ->
            viewModel.refreshPermissionsList()
            onPermissionCallback(type)
        }
    )
}

@Composable
private fun ScreenContent(
    permissions: Set<Permission> = emptySet(),
    onNavigateUp: () -> Unit = {},
    onPermissionCallback: (type: PermissionType) -> Unit = {},
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = pluralStringResource(R.plurals.permissions_required, permissions.size),
                navigationIcon = windowAdaptiveInfo.adaptiveNavigationIcon,
                onNavigateUp = onNavigateUp
            )
        }
    ) { paddingValues ->
        PermissionList(
            modifier = Modifier.padding(paddingValues),
            permissions = permissions,
            onPermissionCallback = onPermissionCallback
        )
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
    }.toSet()
    PreviewTemplate {
        ScreenContent(permissions = permissions)
    }
}
