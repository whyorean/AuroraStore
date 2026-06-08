/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.tv.ui.details

import android.content.ActivityNotFoundException
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.core.text.HtmlCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.aurora.extensions.requiresGMS
import com.aurora.extensions.toast
import com.aurora.store.R
import com.aurora.store.compose.composable.SectionHeader
import com.aurora.store.compose.composable.app.AnimatedAppIcon
import com.aurora.store.compose.ui.details.composable.Changelog
import com.aurora.store.compose.ui.details.composable.Compatibility
import com.aurora.store.compose.ui.details.composable.DataSafety
import com.aurora.store.compose.ui.details.composable.DeveloperDetails
import com.aurora.store.compose.ui.details.composable.Privacy
import com.aurora.store.compose.ui.details.composable.RatingAndReviews
import com.aurora.store.compose.ui.details.composable.Screenshots
import com.aurora.store.compose.ui.details.composable.Tags
import com.aurora.store.data.model.AppState
import com.aurora.store.util.PackageUtil
import com.aurora.store.viewmodel.details.AppDetailsViewModel
import com.aurora.store.viewmodel.details.ExodusViewModel
import kotlinx.coroutines.android.awaitFrame

@Composable
fun AppDetailsScreen(
    packageName: String,
    viewModel: AppDetailsViewModel = hiltViewModel(key = packageName),
    exodusViewModel: ExodusViewModel = hiltViewModel(key = "$packageName/exodus")
) {
    val context = LocalContext.current
    val app by viewModel.app.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val featuredReviews by viewModel.featuredReviews.collectAsStateWithLifecycle()
    val dataSafetyReport by viewModel.dataSafetyReport.collectAsStateWithLifecycle()
    val exodusReport by viewModel.exodusReport.collectAsStateWithLifecycle()
    val plexusScores by viewModel.plexusScores.collectAsStateWithLifecycle()
    val trackers by exodusViewModel.trackers.collectAsStateWithLifecycle()
    val installError by viewModel.installError.collectAsStateWithLifecycle()

    LaunchedEffect(packageName) { viewModel.fetchAppDetails(packageName) }

    if (app == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(
                dimensionResource(R.dimen.tv_content_padding)
            ),
            verticalArrangement = Arrangement.spacedBy(
                dimensionResource(R.dimen.spacing_large),
                Alignment.CenterVertically
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val currentState = state
            if (currentState is AppState.Error) {
                currentState.message?.let { Text(it) }
                Button(onClick = { viewModel.fetchAppDetails(packageName) }) {
                    Text(stringResource(R.string.action_retry))
                }
            } else {
                CircularProgressIndicator()
                Text(stringResource(R.string.loading))
            }
        }
        return
    }

    val loadedApp = app!!
    val focusRequester = remember { FocusRequester() }

    var showFullDescription by remember { mutableStateOf(false) }
    var showPermissions by remember { mutableStateOf(false) }
    var showTrackers by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        awaitFrame()
        runCatching { focusRequester.requestFocus() }
    }

    // Resolve tracker names lazily, only when the privacy section is expanded.
    LaunchedEffect(showTrackers, exodusReport) {
        if (showTrackers) exodusReport?.let { exodusViewModel.getExodusTrackersFromReport(it) }
    }

    installError?.let { err ->
        InstallErrorDialog(
            app = loadedApp,
            error = err.error,
            extra = err.extra,
            onDismiss = viewModel::dismissInstallError
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = dimensionResource(R.dimen.tv_content_padding)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_large))
    ) {
        item {
            Row(
                modifier = Modifier.padding(
                    horizontal = dimensionResource(R.dimen.tv_content_padding)
                ),
                horizontalArrangement = Arrangement.spacedBy(
                    dimensionResource(R.dimen.spacing_large)
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedAppIcon(
                    modifier = Modifier.size(dimensionResource(R.dimen.tv_detail_icon_size)),
                    iconUrl = loadedApp.iconArtwork.url,
                    progress = state.progress(),
                    inProgress = state.inProgress()
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(
                        dimensionResource(R.dimen.spacing_xsmall)
                    )
                ) {
                    Text(
                        text = loadedApp.displayName,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = loadedApp.developerName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = statusText(loadedApp.versionName, loadedApp.versionCode, state),
                        style = MaterialTheme.typography.bodySmall
                    )
                    InstallButton(
                        state = state,
                        onInstall = { viewModel.enqueueDownload(loadedApp) },
                        onCancel = { viewModel.cancelDownload(loadedApp) },
                        onOpen = {
                            val launchIntent = PackageUtil.getLaunchIntent(context, packageName)
                            if (launchIntent != null) {
                                try {
                                    context.startActivity(launchIntent)
                                } catch (_: ActivityNotFoundException) {
                                    context.toast(R.string.unable_to_open)
                                }
                            } else {
                                context.toast(R.string.unable_to_open)
                            }
                        },
                        modifier = Modifier.focusRequester(focusRequester)
                    )
                }
            }
        }

        item { Tags(app = loadedApp) }

        if (loadedApp.changes.isNotBlank()) {
            item { Changelog(changelog = loadedApp.changes) }
        }

        // More about app — clickable header expands the full description inline.
        item {
            Column {
                SectionHeader(
                    title = stringResource(R.string.details_more_about_app),
                    subtitle = loadedApp.shortDescription,
                    onClick = { showFullDescription = !showFullDescription }
                )
                if (showFullDescription) {
                    Text(
                        text = HtmlCompat.fromHtml(
                            loadedApp.description,
                            HtmlCompat.FROM_HTML_MODE_LEGACY
                        ).toString().trim(),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(
                            horizontal = dimensionResource(R.dimen.tv_content_padding)
                        )
                    )
                }
            }
        }

        if (loadedApp.screenshots.isNotEmpty()) {
            item { Screenshots(screenshots = loadedApp.screenshots) }
        }

        item {
            RatingAndReviews(rating = loadedApp.rating, featuredReviews = featuredReviews)
        }

        item { Compatibility(needsGms = loadedApp.requiresGMS(), plexusScores = plexusScores) }

        // Permissions — clickable header expands the requested-permission list inline.
        item {
            Column {
                SectionHeader(
                    title = stringResource(R.string.details_permission),
                    subtitle = if (loadedApp.permissions.isNotEmpty()) {
                        stringResource(R.string.permissions_requested, loadedApp.permissions.size)
                    } else {
                        stringResource(R.string.details_no_permission)
                    },
                    onClick = if (loadedApp.permissions.isNotEmpty()) {
                        { showPermissions = !showPermissions }
                    } else {
                        null
                    }
                )
                if (showPermissions) {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = dimensionResource(R.dimen.tv_content_padding)
                        )
                    ) {
                        loadedApp.permissions.forEach { permission ->
                            Text(text = permission, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        dataSafetyReport?.let { report ->
            item { DataSafety(report = report, privacyPolicyUrl = loadedApp.privacyPolicyUrl) }
        }

        // Privacy (Exodus) — clickable header expands the resolved tracker list inline.
        item {
            val hasTrackers = (exodusReport?.trackers?.isNotEmpty() == true)
            Column {
                Privacy(
                    report = exodusReport,
                    onNavigateToDetailsExodus = if (hasTrackers) {
                        { showTrackers = !showTrackers }
                    } else {
                        null
                    }
                )
                if (showTrackers) {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = dimensionResource(R.dimen.tv_content_padding)
                        )
                    ) {
                        trackers.forEach { tracker ->
                            Text(text = tracker.name, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        item {
            DeveloperDetails(
                address = loadedApp.developerAddress,
                website = loadedApp.developerWebsite,
                email = loadedApp.developerEmail
            )
        }
    }
}

@Composable
private fun statusText(versionName: String, versionCode: Long, state: AppState): String =
    when (state) {
        is AppState.Downloading -> "${state.progress.toInt()}%"
        is AppState.Installing -> stringResource(R.string.action_installing)
        is AppState.Queued -> stringResource(R.string.status_queued)
        is AppState.Purchasing -> stringResource(R.string.preparing_to_download)
        is AppState.Verifying -> stringResource(R.string.verifying_downloads)
        else -> stringResource(R.string.version, versionName, versionCode)
    }
