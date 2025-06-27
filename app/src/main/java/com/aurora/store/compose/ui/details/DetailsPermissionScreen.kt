/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details

import android.content.pm.PermissionInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowWidthSizeClass
import com.aurora.extensions.adaptiveNavigationIcon
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.compose.composables.InfoComposable
import com.aurora.store.compose.composables.TopAppBarComposable
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.viewmodel.details.AppDetailsViewModel
import com.aurora.store.viewmodel.details.DetailsPermissionViewModel
import java.util.Locale

@Composable
fun DetailsPermissionScreen(
    onNavigateUp: () -> Unit,
    appDetailsViewModel: AppDetailsViewModel = hiltViewModel(),
    detailsPermissionViewModel: DetailsPermissionViewModel = hiltViewModel { factory: DetailsPermissionViewModel.Factory ->
        factory.create(appDetailsViewModel.app.value!!.permissions)
    },
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo()
) {
    val app by appDetailsViewModel.app.collectAsStateWithLifecycle()
    val permissionsInfo by detailsPermissionViewModel.permissionsInfo.collectAsStateWithLifecycle()

    val topAppBarTitle = when (windowAdaptiveInfo.windowSizeClass.windowWidthSizeClass) {
        WindowWidthSizeClass.COMPACT -> app!!.displayName
        else -> stringResource(R.string.details_permission)
    }

    ScreenContent(
        topAppBarTitle = topAppBarTitle,
        onNavigateUp = onNavigateUp,
        permissionsInfo = permissionsInfo
    )
}

@Composable
private fun ScreenContent(
    topAppBarTitle: String? = null,
    permissionsInfo: Map<String, PermissionInfo?> = emptyMap(),
    onNavigateUp: () -> Unit = {},
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo()
) {
    val packageManager = LocalContext.current.packageManager

    Scaffold(
        topBar = {
            TopAppBarComposable(
                title = topAppBarTitle,
                navigationIcon = windowAdaptiveInfo.adaptiveNavigationIcon,
                onNavigateUp = onNavigateUp
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = dimensionResource(R.dimen.padding_medium)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_medium))
        ) {
            items(items = permissionsInfo.keys.toList(), key = { it }) { permission ->
                // Bail out if this is not a known permission for the OS
                val permissionInfo = permissionsInfo.getValue(permission) ?: return@items

                InfoComposable(
                    title = AnnotatedString(
                        text = permissionInfo.loadLabel(packageManager)
                            .toString()
                            .replaceFirstChar {
                                if (it.isLowerCase()) {
                                    it.titlecase(Locale.getDefault())
                                } else {
                                    it.toString()
                                }
                            }
                    ),
                    description = AnnotatedString(
                        text = permissionInfo.loadDescription(packageManager)?.toString()
                            ?: stringResource(R.string.no_description)
                    )
                )
            }
        }
    }
}

@Preview
@Composable
private fun DetailsPermissionScreenPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    ScreenContent(
        topAppBarTitle = app.displayName
    )
}
