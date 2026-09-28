/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.aurora.store.R
import com.aurora.store.compose.composable.auroraSegmentedListItemShapes
import com.aurora.store.compose.composable.app.AnimatedAppIcon
import com.aurora.store.compose.preview.ThemePreviewProvider
import com.aurora.store.data.model.DownloadStatus
import com.aurora.store.data.room.download.Download
import com.aurora.store.data.room.update.Update
import com.aurora.store.util.CommonUtil

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppUpdateItem(
    modifier: Modifier = Modifier,
    update: Update,
    download: Download? = null,
    isChecking: Boolean = false,
    itemIndex: Int = 0,
    itemCount: Int = 1,
    onClick: () -> Unit = {},
    onUpdate: () -> Unit = {},
    onCancel: () -> Unit = {},
    onUnignore: (() -> Unit)? = null
) {
    val inProgress = isChecking ||
        (download != null && !download.isFinished && !download.isAwaitingInstall)
    val installing = download?.status == DownloadStatus.INSTALLING
    val progress = if (download?.status == DownloadStatus.DOWNLOADING) {
        download.progress.toFloat()
    } else {
        0f
    }

    SegmentedListItem(
        selected = false,
        onClick = onClick,
        shapes = auroraSegmentedListItemShapes(index = itemIndex, count = itemCount),
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        contentPadding = PaddingValues(
            horizontal = dimensionResource(R.dimen.spacing_large),
            vertical = dimensionResource(R.dimen.spacing_small)
        ),
        leadingContent = {
            AnimatedAppIcon(
                modifier = Modifier.requiredSize(dimensionResource(R.dimen.icon_size_medium)),
                iconUrl = update.iconURL,
                inProgress = inProgress,
                progress = progress
            )
        },
        supportingContent = {
            Column {
                Text(
                    text = update.developerName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${update.versionName}  •  ${CommonUtil.addSiPrefix(update.size)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        trailingContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(
                    dimensionResource(R.dimen.spacing_xsmall)
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when {
                    onUnignore != null -> {
                        OutlinedButton(onClick = onUnignore) {
                            Text(stringResource(R.string.action_unignore))
                        }
                    }

                    installing -> {
                        OutlinedButton(onClick = {}, enabled = false) {
                            Text(stringResource(R.string.action_installing))
                        }
                    }

                    inProgress -> {
                        OutlinedButton(onClick = onCancel) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }

                    else -> {
                        Button(onClick = onUpdate) {
                            Text(stringResource(R.string.action_update))
                        }
                    }
                }
            }
        }
    ) {
        Text(
            text = update.displayName,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun AppUpdateItemPreview() {
    val update = Update(
        packageName = "com.example.app",
        versionCode = 100,
        versionName = "1.0.0",
        displayName = "Example App",
        iconURL = "",
        changelog = "• Bug fixes<br>• Performance improvements",
        id = 1,
        developerName = "Developer",
        size = 5_000_000,
        updatedOn = "May 2025",
        hasValidCert = true,
        offerType = 1,
        fileList = emptyList(),
        sharedLibs = emptyList()
    )
    AppUpdateItem(update = update)
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun AppUpdateItemDownloadingPreview() {
    val update = Update(
        packageName = "com.example.app",
        versionCode = 100,
        versionName = "1.0.0",
        displayName = "Example App",
        iconURL = "",
        changelog = "",
        id = 1,
        developerName = "Developer",
        size = 5_000_000,
        updatedOn = "May 2025",
        hasValidCert = true,
        offerType = 1,
        fileList = emptyList(),
        sharedLibs = emptyList()
    )
    val download = Download.fromUpdate(update).copy(
        status = DownloadStatus.DOWNLOADING,
        progress = 45
    )
    AppUpdateItem(update = update, download = download)
}
