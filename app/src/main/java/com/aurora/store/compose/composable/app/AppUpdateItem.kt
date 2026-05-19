/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.aurora.store.compose.preview.ThemePreviewProvider
import com.aurora.store.data.model.DownloadStatus
import com.aurora.store.data.room.download.Download
import com.aurora.store.data.room.update.Update
import com.aurora.store.util.CommonUtil

@Composable
fun AppUpdateItem(
    modifier: Modifier = Modifier,
    update: Update,
    download: Download? = null,
    onClick: () -> Unit = {},
    onUpdate: () -> Unit = {},
    onCancel: () -> Unit = {},
    onUnignore: (() -> Unit)? = null
) {
    val inProgress = download != null && !download.isFinished
    val pendingInstall = download?.status == DownloadStatus.COMPLETED
    val progress = if (download?.status == DownloadStatus.DOWNLOADING) {
        download.progress.toFloat()
    } else {
        0f
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = dimensionResource(R.dimen.spacing_medium),
                vertical = dimensionResource(R.dimen.spacing_small)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.requiredSize(dimensionResource(R.dimen.icon_size_medium))) {
            AnimatedAppIcon(
                modifier = Modifier.requiredSize(dimensionResource(R.dimen.icon_size_medium)),
                iconUrl = update.iconURL,
                inProgress = inProgress || pendingInstall,
                progress = progress
            )
        }
        Spacer(Modifier.width(dimensionResource(R.dimen.spacing_medium)))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = update.displayName,
                style = MaterialTheme.typography.bodyMedium,
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
                text = "${update.versionName}  •  ${CommonUtil.addSiPrefix(update.size)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(dimensionResource(R.dimen.spacing_small)))
        when {
            onUnignore != null -> {
                OutlinedButton(onClick = onUnignore) {
                    Text(stringResource(R.string.action_unignore))
                }
            }

            pendingInstall -> {
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
