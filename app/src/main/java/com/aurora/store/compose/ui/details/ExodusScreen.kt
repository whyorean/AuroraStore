/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurora.Constants.EXODUS_REPORT_URL
import com.aurora.Constants.EXODUS_SUBMIT_PAGE
import com.aurora.extensions.adaptiveNavigationIcon
import com.aurora.extensions.browse
import com.aurora.extensions.isWindowCompact
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.compose.composable.Placeholder
import com.aurora.store.compose.composable.ScrollHint
import com.aurora.store.compose.composable.SectionHeader
import com.aurora.store.compose.composable.TopAppBar
import com.aurora.store.compose.composable.details.ExodusListItem
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.ThemePreviewProvider
import com.aurora.store.data.model.ExodusTracker
import com.aurora.store.data.model.Report
import com.aurora.store.data.model.formatExodusDate
import com.aurora.store.viewmodel.details.AppDetailsViewModel
import com.aurora.store.viewmodel.details.ExodusViewModel

@Composable
fun ExodusScreen(
    packageName: String,
    appDetailsViewModel: AppDetailsViewModel = hiltViewModel(key = packageName),
    exodusViewModel: ExodusViewModel = hiltViewModel(key = "$packageName/exodus"),
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfoV2()
) {
    val context = LocalContext.current

    val app by appDetailsViewModel.app.collectAsStateWithLifecycle()
    val report by appDetailsViewModel.exodusReport.collectAsStateWithLifecycle()
    val reports by appDetailsViewModel.exodusReports.collectAsStateWithLifecycle()

    val topAppBarTitle = when {
        windowAdaptiveInfo.isWindowCompact -> app!!.displayName
        else -> stringResource(R.string.details_privacy)
    }

    when (report) {
        null -> {
            ScreenContentError(
                topAppBarTitle = topAppBarTitle,
                onRequestAnalysis = { context.browse("${EXODUS_SUBMIT_PAGE}${app!!.packageName}") }
            )
        }

        else -> {
            ScreenContentReport(
                topAppBarTitle = topAppBarTitle,
                reports = reports,
                onResolveTrackers = { selected -> exodusViewModel.resolveTrackers(selected) },
                onViewReport = { selected -> context.browse(EXODUS_REPORT_URL + selected.id) },
                onRequestAnalysis = { context.browse("${EXODUS_SUBMIT_PAGE}${app!!.packageName}") }
            )
        }
    }
}

@Composable
private fun ScreenContentReport(
    topAppBarTitle: String? = null,
    reports: List<Report> = emptyList(),
    onResolveTrackers: suspend (Report) -> List<ExodusTracker> = { emptyList() },
    onViewReport: (Report) -> Unit = {},
    onRequestAnalysis: () -> Unit = {},
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfoV2()
) {
    var selectedReport by remember { mutableStateOf<Report?>(null) }

    selectedReport?.let { selected ->
        ExodusReportDialog(
            report = selected,
            onResolveTrackers = onResolveTrackers,
            onViewReport = { onViewReport(selected) },
            onDismiss = { selectedReport = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = topAppBarTitle,
                navigationIcon = windowAdaptiveInfo.adaptiveNavigationIcon
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
        val listState = rememberLazyListState()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                state = listState
            ) {
                item(key = "history_header") {
                    SectionHeader(title = stringResource(R.string.exodus_report_history))
                }
                itemsIndexed(
                    items = reports,
                    key = { _, item -> "report-${item.id}" }
                ) { index, historyReport ->
                    VersionRow(
                        report = historyReport,
                        isLatest = index == 0,
                        onClick = { selectedReport = historyReport }
                    )
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
private fun VersionRow(report: Report, isLatest: Boolean, onClick: () -> Unit) {
    val spacing = dimensionResource(R.dimen.spacing_small)
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                Text(
                    stringResource(
                        R.string.exodus_history_version,
                        report.version,
                        report.versionCode
                    )
                )
                if (isLatest) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = stringResource(R.string.exodus_latest),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(
                                horizontal = dimensionResource(R.dimen.spacing_small),
                                vertical = 2.dp
                            )
                        )
                    }
                }
            }
        },
        supportingContent = {
            Text(
                stringResource(
                    R.string.exodus_history_subtitle,
                    formatExodusDate(report.creationDate),
                    report.trackers.size
                )
            )
        }
    )
}

@Composable
private fun ExodusReportDialog(
    report: Report,
    onResolveTrackers: suspend (Report) -> List<ExodusTracker>,
    onViewReport: () -> Unit,
    onDismiss: () -> Unit
) {
    val trackers by produceState<List<ExodusTracker>?>(initialValue = null, report) {
        value = onResolveTrackers(report)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(report.version) },
        text = {
            val resolved = trackers
            when {
                resolved == null -> {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                resolved.isEmpty() -> Text(stringResource(R.string.exodus_no_tracker))

                else -> {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        resolved.forEach { tracker -> ExodusListItem(tracker = tracker) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        },
        dismissButton = {
            TextButton(onClick = onViewReport) {
                Text(stringResource(R.string.exodus_view_report))
            }
        }
    )
}

/**
 * Composable to display errors related to fetching app details
 */
@Composable
private fun ScreenContentError(
    topAppBarTitle: String? = null,
    onRequestAnalysis: () -> Unit = {},
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfoV2()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = topAppBarTitle,
                navigationIcon = windowAdaptiveInfo.adaptiveNavigationIcon
            )
        }
    ) { paddingValues ->
        Placeholder(
            modifier = Modifier.padding(paddingValues),
            painter = painterResource(R.drawable.ic_disclaimer),
            message = stringResource(R.string.failed_to_fetch_report),
            actionLabel = stringResource(R.string.action_request_analysis),
            onAction = onRequestAnalysis
        )
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview
@Composable
private fun ExodusScreenPreviewReport(@PreviewParameter(AppPreviewProvider::class) app: App) {
    ScreenContentReport(
        topAppBarTitle = app.displayName,
        reports = listOf(
            Report(id = 2, version = "5.2.0", creationDate = "2024-03-11", trackers = listOf(1, 2)),
            Report(id = 1, version = "5.1.0", creationDate = "2023-11-02", trackers = listOf(1))
        )
    )
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview
@Composable
private fun ExodusScreenPreviewError(@PreviewParameter(AppPreviewProvider::class) app: App) {
    ScreenContentError(topAppBarTitle = app.displayName)
}
