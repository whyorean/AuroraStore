/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.tv.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.aurora.store.R
import com.aurora.store.compose.composable.app.AnimatedAppIcon
import com.aurora.store.data.model.DownloadStatus
import com.aurora.store.data.room.download.Download
import com.aurora.store.data.room.update.Update
import com.aurora.store.util.CommonUtil
import com.aurora.store.viewmodel.all.UpdatesViewModel

@Composable
fun UpdatesSection(
    onAppClick: (String) -> Unit,
    viewModel: UpdatesViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    // updates is StateFlow<List<Update>?> — null means the initial (pre-check) loading state.
    val updates by viewModel.updates.collectAsStateWithLifecycle()
    val downloads by viewModel.downloadsList.collectAsStateWithLifecycle()
    val ignored by viewModel.ignoredUpdates.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.fetchUpdates() }

    // Pair each update with its in-flight download (matched on package + versionCode) so each row
    // can reflect live progress/state, mirroring the phone UpdatesScreen.
    val updateMap = remember(updates, downloads) {
        updates?.associateWith { update ->
            downloads.find {
                it.packageName == update.packageName && it.versionCode == update.versionCode
            }
        }
    }

    when {
        updateMap == null -> {
            Column(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(
                    dimensionResource(R.dimen.spacing_large),
                    Alignment.CenterVertically
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Text(stringResource(R.string.loading))
            }
        }

        updateMap.isEmpty() && ignored.isEmpty() -> {
            Column(
                modifier = modifier.fillMaxSize().padding(
                    dimensionResource(R.dimen.spacing_xlarge)
                ),
                verticalArrangement = Arrangement.spacedBy(
                    dimensionResource(R.dimen.spacing_large),
                    Alignment.CenterVertically
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.details_no_updates))
                Button(onClick = { viewModel.fetchUpdates() }) {
                    Text(stringResource(R.string.check_updates))
                }
            }
        }

        else -> {
            val entries = updateMap.entries.toList()
            val anyActive = entries.any { it.value.isActive() }

            LazyColumn(
                modifier = modifier.fillMaxSize(),
                // contentPadding (not Modifier.padding) keeps focused rows from clipping at the edges.
                contentPadding = PaddingValues(dimensionResource(R.dimen.spacing_xlarge)),
                verticalArrangement = Arrangement.spacedBy(
                    dimensionResource(R.dimen.spacing_medium)
                )
            ) {
                if (entries.isNotEmpty()) {
                    item(key = "header") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val countLabel = "${entries.size} " + stringResource(
                                if (entries.size == 1) {
                                    R.string.update_available
                                } else {
                                    R.string.updates_available
                                }
                            )
                            Text(
                                text = countLabel,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Button(
                                onClick = {
                                    if (anyActive) {
                                        viewModel.cancelAll()
                                    } else {
                                        viewModel.downloadAll(entries.map { it.key })
                                    }
                                }
                            ) {
                                Text(
                                    stringResource(
                                        if (anyActive) {
                                            R.string.download_cancel_all
                                        } else {
                                            R.string.action_update_all
                                        }
                                    )
                                )
                            }
                        }
                    }

                    items(items = entries, key = { "update-${it.key.packageName}" }) { entry ->
                        UpdateItem(
                            update = entry.key,
                            download = entry.value,
                            onClick = { onAppClick(entry.key.packageName) },
                            onUpdate = { viewModel.download(entry.key) },
                            onCancel = { viewModel.cancelDownload(entry.key.packageName) }
                        )
                    }
                }

                if (ignored.isNotEmpty()) {
                    item(key = "header_ignored") {
                        Text(
                            text = stringResource(R.string.updates_ignored_header),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(
                                top = dimensionResource(R.dimen.spacing_medium)
                            )
                        )
                    }
                    items(items = ignored, key = { "ignored-${it.packageName}" }) { update ->
                        UpdateItem(
                            update = update,
                            download = null,
                            onClick = { onAppClick(update.packageName) },
                            onUpdate = { viewModel.unignore(update.packageName) },
                            updateLabel = R.string.action_unignore
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateItem(
    update: Update,
    download: Download?,
    onClick: () -> Unit,
    onUpdate: () -> Unit,
    onCancel: () -> Unit = {},
    updateLabel: Int = R.string.action_update
) {
    // Only INSTALLING shows as "Installing"; a downloaded-but-not-installed (finished) app falls
    // back to the update action, matching the phone AppUpdateItem.
    val inProgress = download != null && !download.isFinished
    val installing = download?.status == DownloadStatus.INSTALLING
    val progress = if (download?.status == DownloadStatus.DOWNLOADING) {
        download.progress.toFloat()
    } else {
        0f
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
    ) {
        val focusShape = RoundedCornerShape(dimensionResource(R.dimen.radius_medium))
        Surface(
            onClick = onClick,
            modifier = Modifier.weight(1f),
            shape = ClickableSurfaceDefaults.shape(focusShape),
            // Keep the same fill in every state: tv-material's bright focused container colour washed
            // out the secondary text. Focus is conveyed by the outline alone.
            colors = ClickableSurfaceDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                pressedContainerColor = MaterialTheme.colorScheme.surface
            ),
            // A wide row scaled up on focus overdraws the action button and the side nav; keep it at
            // rest size and mark focus with a card-like outline instead.
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(
                        width = dimensionResource(R.dimen.tv_focus_border),
                        color = MaterialTheme.colorScheme.border
                    ),
                    shape = focusShape
                )
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(R.dimen.spacing_medium)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    dimensionResource(R.dimen.spacing_medium)
                )
            ) {
                AnimatedAppIcon(
                    modifier = Modifier.size(dimensionResource(R.dimen.icon_size_medium)),
                    iconUrl = update.iconURL,
                    inProgress = inProgress,
                    progress = progress
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = update.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = update.developerName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = statusLine(update, download),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        when {
            installing -> Button(onClick = {}, enabled = false) {
                Text(stringResource(R.string.action_installing))
            }

            inProgress -> Button(onClick = onCancel) {
                Text(stringResource(R.string.action_cancel))
            }

            else -> Button(onClick = onUpdate) {
                Text(stringResource(updateLabel))
            }
        }
    }
}

/**
 * Supporting line for an update row: shows live download state/progress while a download is in
 * flight, otherwise the target version and download size.
 */
@Composable
private fun statusLine(update: Update, download: Download?): String = when {
    download == null || download.isFinished -> {
        "${update.versionName}  •  ${CommonUtil.addSiPrefix(update.size)}"
    }

    download.status == DownloadStatus.DOWNLOADING -> {
        "${stringResource(download.status.localized)}  •  ${download.progress}%"
    }

    else -> stringResource(download.status.localized)
}

private fun Download?.isActive(): Boolean {
    if (this == null) return false
    return !isFinished || status == DownloadStatus.COMPLETED
}
