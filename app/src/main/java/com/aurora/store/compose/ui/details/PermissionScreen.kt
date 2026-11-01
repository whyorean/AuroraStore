/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details

import android.content.pm.PermissionInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurora.extensions.adaptiveNavigationIcon
import com.aurora.extensions.isWindowCompact
import com.aurora.gplayapi.model.App
import com.aurora.store.R
import com.aurora.store.compose.composable.Info
import com.aurora.store.compose.composable.ScrollHint
import com.aurora.store.compose.composable.SectionHeader
import com.aurora.store.compose.composable.TopAppBar
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.ThemePreviewProvider
import com.aurora.store.viewmodel.details.AppDetailsViewModel
import com.aurora.store.viewmodel.details.PermissionViewModel

@Composable
fun PermissionScreen(
    packageName: String,
    appDetailsViewModel: AppDetailsViewModel = hiltViewModel(key = packageName),
    permissionViewModel: PermissionViewModel = hiltViewModel(
        key = "$packageName/permission",
        creationCallback = { factory: PermissionViewModel.Factory ->
            val app = appDetailsViewModel.app.value!!
            factory.create(app.packageName, app.permissions)
        }
    ),
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfoV2()
) {
    val app by appDetailsViewModel.app.collectAsStateWithLifecycle()
    val permissionsInfo by permissionViewModel.permissionsInfo.collectAsStateWithLifecycle()
    val newPermissions by permissionViewModel.newPermissions.collectAsStateWithLifecycle()
    val groups by permissionViewModel.groups.collectAsStateWithLifecycle()

    val topAppBarTitle = when {
        windowAdaptiveInfo.isWindowCompact -> app!!.displayName
        else -> stringResource(R.string.details_permission)
    }

    ScreenContent(
        topAppBarTitle = topAppBarTitle,
        permissionsInfo = permissionsInfo,
        newPermissions = newPermissions,
        groups = groups
    )
}

@Composable
private fun ScreenContent(
    topAppBarTitle: String? = null,
    permissionsInfo: Map<String, PermissionInfo> = emptyMap(),
    newPermissions: Set<String> = emptySet(),
    groups: List<PermissionViewModel.Group> = emptyList(),
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfoV2()
) {
    val packageManager = LocalContext.current.packageManager

    Scaffold(
        topBar = {
            TopAppBar(
                title = topAppBarTitle,
                navigationIcon = windowAdaptiveInfo.adaptiveNavigationIcon
            )
        }
    ) { paddingValues ->
        val listState = rememberLazyListState()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = dimensionResource(R.dimen.spacing_medium)),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(
                    dimensionResource(R.dimen.spacing_medium)
                )
            ) {
                groups.forEach { group ->
                    item(key = "group-${group.label}") {
                        SectionHeader(title = group.label)
                    }
                    items(items = group.permissions, key = { it }) { permission ->
                        val permissionInfo = permissionsInfo.getValue(permission)
                        val label = permissionInfo.loadLabel(packageManager)
                            .toString()
                            .replaceFirstChar {
                                if (it.isLowerCase()) {
                                    it.titlecase(LocalLocale.current.platformLocale)
                                } else {
                                    it.toString()
                                }
                            }
                        Info(
                            title = AnnotatedString(text = label),
                            titleMaxLines = Int.MAX_VALUE,
                            trailing = if (permission in newPermissions) {
                                { NewChip() }
                            } else {
                                null
                            },
                            description = AnnotatedString(
                                text = permissionInfo.loadDescription(packageManager)?.toString()
                                    ?: stringResource(R.string.no_description)
                            )
                        )
                    }
                }
            }
            ScrollHint(
                listState = listState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun NewChip() {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Text(
            modifier = Modifier.padding(
                horizontal = dimensionResource(R.dimen.spacing_small),
                vertical = dimensionResource(R.dimen.spacing_xsmall)
            ),
            text = stringResource(R.string.permission_new),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview
@Composable
private fun PermissionScreenPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    ScreenContent(
        topAppBarTitle = app.displayName
    )
}
