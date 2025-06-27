/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowWidthSizeClass
import com.aurora.Constants.EXODUS_REPORT_URL
import com.aurora.extensions.adaptiveNavigationIcon
import com.aurora.extensions.browse
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.compose.composables.HeaderComposable
import com.aurora.store.compose.composables.TopAppBarComposable
import com.aurora.store.compose.composables.details.ExodusComposable
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.data.model.ExodusTracker
import com.aurora.store.viewmodel.details.AppDetailsViewModel
import com.aurora.store.viewmodel.details.DetailsExodusViewModel

@Composable
fun DetailsExodusScreen(
    onNavigateUp: () -> Unit,
    appDetailsViewModel: AppDetailsViewModel = hiltViewModel(),
    detailsExodusViewModel: DetailsExodusViewModel = hiltViewModel { factory: DetailsExodusViewModel.Factory ->
        factory.create(appDetailsViewModel.exodusReport.value!!)
    },
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo()
) {
    val app by appDetailsViewModel.app.collectAsStateWithLifecycle()
    val exodusReport by appDetailsViewModel.exodusReport.collectAsStateWithLifecycle()
    val trackers by detailsExodusViewModel.trackers.collectAsStateWithLifecycle()

    val topAppBarTitle = when (windowAdaptiveInfo.windowSizeClass.windowWidthSizeClass) {
        WindowWidthSizeClass.COMPACT -> app!!.displayName
        else -> stringResource(R.string.details_privacy)
    }

    ScreenContent(
        topAppBarTitle = topAppBarTitle,
        id = exodusReport!!.id,
        version = exodusReport!!.version,
        trackers = trackers,
        onNavigateUp = onNavigateUp,
    )
}

@Composable
private fun ScreenContent(
    topAppBarTitle: String? = null,
    id: Int = -1,
    version: String = String(),
    trackers: List<ExodusTracker> = emptyList(),
    onNavigateUp: () -> Unit = {},
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo()
) {
    val context = LocalContext.current

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
                .padding(horizontal = dimensionResource(R.dimen.padding_medium))
        ) {
            stickyHeader {
                HeaderComposable(
                    title = stringResource(R.string.exodus_report_trackers, trackers.size, version),
                    subtitle = stringResource(R.string.exodus_view_report),
                    onClick = { context.browse(EXODUS_REPORT_URL + id) }
                )
            }

            items(items = trackers, key = { item -> item.id }) { tracker ->
                ExodusComposable(tracker = tracker)
            }
        }
    }
}

@Preview
@Composable
private fun DetailsExodusScreenPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    ScreenContent(
        topAppBarTitle = app.displayName,
        version = app.versionName
    )
}
