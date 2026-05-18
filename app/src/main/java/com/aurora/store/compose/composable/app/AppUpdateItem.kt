/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
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
    onLongClick: () -> Unit = {},
    onUpdate: () -> Unit = {},
    onCancel: () -> Unit = {}
) {
    val inProgress = download != null && download.status !in DownloadStatus.finished
    val progress = if (download?.status == DownloadStatus.DOWNLOADING) {
        download.progress.toFloat()
    } else {
        0f
    }
    var changelogExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(
                    horizontal = dimensionResource(R.dimen.padding_medium),
                    vertical = dimensionResource(R.dimen.padding_small)
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.requiredSize(dimensionResource(R.dimen.icon_size_medium))) {
                AnimatedAppIcon(
                    modifier = Modifier.requiredSize(dimensionResource(R.dimen.icon_size_medium)),
                    iconUrl = update.iconURL,
                    inProgress = inProgress,
                    progress = progress
                )
            }
            Spacer(Modifier.width(dimensionResource(R.dimen.margin_small)))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = update.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = update.developerName,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "${CommonUtil.addSiPrefix(update.size)}  •  ${update.updatedOn}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "${update.versionName} (${update.versionCode})",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.width(dimensionResource(R.dimen.margin_small)))
            if (inProgress) {
                OutlinedButton(onClick = onCancel) {
                    Text(stringResource(R.string.action_cancel))
                }
            } else {
                Button(onClick = onUpdate) {
                    Text(stringResource(R.string.action_update))
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { changelogExpanded = !changelogExpanded }
                .padding(horizontal = dimensionResource(R.dimen.padding_medium)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { changelogExpanded = !changelogExpanded }) {
                Icon(
                    painter = painterResource(
                        if (changelogExpanded) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down
                    ),
                    contentDescription = stringResource(R.string.expand)
                )
            }
        }

        AnimatedVisibility(visible = changelogExpanded) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dimensionResource(R.dimen.padding_medium),
                        vertical = dimensionResource(R.dimen.padding_small)
                    ),
                shape = RoundedCornerShape(dimensionResource(R.dimen.radius_small))
            ) {
                Text(
                    text = if (update.changelog.isNotEmpty()) {
                        AnnotatedString.fromHtml(update.changelog)
                    } else {
                        AnnotatedString(stringResource(R.string.details_changelog_unavailable))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(dimensionResource(R.dimen.padding_medium))
                )
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
