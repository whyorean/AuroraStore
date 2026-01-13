/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurora.Constants.EXODUS_REPORT_URL
import com.aurora.Constants.EXODUS_SUBMIT_PAGE
import com.aurora.extensions.adaptiveNavigationIcon
import com.aurora.extensions.browse
import com.aurora.extensions.isWindowCompact
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.compose.composable.Error
import com.aurora.store.compose.composable.Header
import com.aurora.store.compose.composable.TopAppBar
import com.aurora.store.compose.composable.details.ExodusListItem
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.PreviewTemplate
import com.aurora.store.data.model.ExodusTracker
import com.aurora.store.data.model.Report
import com.aurora.store.viewmodel.details.AppDetailsViewModel
import com.aurora.store.viewmodel.details.ExodusViewModel

@Composable
fun ExodusScreen(
    packageName: String,
    onNavigateUp: () -> Unit,
    appDetailsViewModel: AppDetailsViewModel = hiltViewModel(key = packageName),
    exodusViewModel: ExodusViewModel = hiltViewModel(key = "$packageName/exodus"),
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo()
) {
    val context = LocalContext.current

    val app by appDetailsViewModel.app.collectAsStateWithLifecycle()
    val report by appDetailsViewModel.exodusReport.collectAsStateWithLifecycle()
    val trackers by exodusViewModel.trackers.collectAsStateWithLifecycle()

    LaunchedEffect(key1 = Unit) {
        report?.let { exodusViewModel.getExodusTrackersFromReport(it) }
    }

    val topAppBarTitle = when {
        windowAdaptiveInfo.isWindowCompact -> app!!.displayName
        else -> stringResource(R.string.details_privacy)
    }

    when (report) {
        null -> {
            ScreenContentError(
                topAppBarTitle = topAppBarTitle,
                onNavigateUp = onNavigateUp,
                onRequestAnalysis = { context.browse("${EXODUS_SUBMIT_PAGE}${app!!.packageName}") }
            )
        }

        else -> {
            ScreenContentReport(
                topAppBarTitle = topAppBarTitle,
                report = report,
                trackers = trackers,
                onNavigateUp = onNavigateUp,
                onRequestAnalysis = { context.browse("${EXODUS_SUBMIT_PAGE}${app!!.packageName}") }
            )
        }
    }
}

@Composable
private fun ScreenContentReport(
    topAppBarTitle: String? = null,
    report: Report? = null,
    trackers: List<ExodusTracker> = emptyList(),
    onNavigateUp: () -> Unit = {},
    onRequestAnalysis: () -> Unit = {},
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo()
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = topAppBarTitle,
                navigationIcon = windowAdaptiveInfo.adaptiveNavigationIcon,
                onNavigateUp = onNavigateUp
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onRequestAnalysis) {
                Icon(
                    painter = painterResource(R.drawable.ic_scan),
                    contentDescription = stringResource(R.string.action_request_analysis)
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = dimensionResource(R.dimen.padding_medium))
        ) {
            stickyHeader {
                Surface(modifier = Modifier.fillMaxWidth()) {
                    Header(
                        title = if (report?.trackers.isNullOrEmpty()) {
                            stringResource(R.string.exodus_no_tracker)
                        } else {
                            stringResource(
                                R.string.exodus_report_trackers,
                                report.trackers.size,
                                report.version
                            )
                        },
                        subtitle = stringResource(R.string.exodus_view_report),
                        onClick = { context.browse(EXODUS_REPORT_URL + report!!.id) }
                    )
                }
            }

            items(items = trackers, key = { item -> item.id }) { tracker ->
                ExodusListItem(tracker = tracker)
            }
        }
    }
}

/**
 * Composable to display errors related to fetching app details
 */
@Composable
private fun ScreenContentError(
    topAppBarTitle: String? = null,
    onNavigateUp: () -> Unit = {},
    onRequestAnalysis: () -> Unit = {},
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = topAppBarTitle,
                navigationIcon = windowAdaptiveInfo.adaptiveNavigationIcon,
                onNavigateUp = onNavigateUp
            )
        }
    ) { paddingValues ->
        Error(
            modifier = Modifier.padding(paddingValues),
            painter = painterResource(R.drawable.ic_disclaimer),
            message = stringResource(R.string.failed_to_fetch_report),
            actionMessage = stringResource(R.string.action_request_analysis),
            onAction = onRequestAnalysis
        )
    }
}

@Preview
@Composable
private fun ExodusScreenPreviewReport(@PreviewParameter(AppPreviewProvider::class) app: App) {
    PreviewTemplate {
        ScreenContentReport(topAppBarTitle = app.displayName)
    }
}

@Preview
@Composable
private fun ExodusScreenPreviewError(@PreviewParameter(AppPreviewProvider::class) app: App) {
    PreviewTemplate {
        ScreenContentError(topAppBarTitle = app.displayName)
    }
}
