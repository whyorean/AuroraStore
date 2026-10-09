/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.ThemePreviewProvider
import com.aurora.store.compose.composable.rememberStoreImageRequest
import com.aurora.store.data.model.DownloadStatus
import com.aurora.store.data.room.download.Download
import com.aurora.store.util.CommonUtil
import com.aurora.store.util.PackageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val cancellableDownloadStates = setOf(
    DownloadStatus.QUEUED,
    DownloadStatus.PURCHASING,
    DownloadStatus.DOWNLOADING
)

private val installingDownloadStates = setOf(
    DownloadStatus.COMPLETED,
    DownloadStatus.VERIFYING,
    DownloadStatus.AWAITING_INSTALL,
    DownloadStatus.INSTALLING
)

private val animatedDownloadStates = cancellableDownloadStates + installingDownloadStates

/** Compact store card used in home discovery carousels. */
@Composable
fun AppListItem(
    modifier: Modifier = Modifier,
    app: App,
    showHomeAction: Boolean = false,
    download: Download? = null,
    isUpdatable: Boolean = false,
    isCancellationPending: Boolean = false,
    onActionClick: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var isInstalled by remember(app.packageName) { mutableStateOf(app.isInstalled) }
    LaunchedEffect(app.packageName, showHomeAction) {
        if (showHomeAction) {
            isInstalled = withContext(Dispatchers.IO) {
                PackageUtil.isInstalled(context, app.packageName)
            }
        }
    }
    LaunchedEffect(download?.status, showHomeAction) {
        if (showHomeAction && download?.status == DownloadStatus.INSTALLED) {
            isInstalled = true
        }
    }

    val canOpen = remember(showHomeAction, app.packageName, isInstalled) {
        showHomeAction && isInstalled && PackageUtil.getLaunchIntent(context, app.packageName) != null
    }
    val status = if (showHomeAction) download?.status else null
    val isCancellable = status != null && status in cancellableDownloadStates
    val isInstalling = status != null && status in installingDownloadStates
    val isAnimated = status != null && status in animatedDownloadStates
    val actionLabel = when {
        isCancellationPending -> R.string.action_cancel
        isCancellable -> R.string.action_cancel
        isInstalling -> R.string.action_installing
        isUpdatable -> R.string.action_update
        isInstalled -> R.string.action_open
        app.isFree -> R.string.action_install
        else -> null
    }
    val isActionEnabled = when {
        isCancellationPending || isInstalling -> false
        isCancellable || isUpdatable -> true
        isInstalled -> canOpen
        else -> true
    }
    val installCount = remember(app.installs, app.downloadString) {
        CommonUtil.addDiPrefix(app.installs)
            ?: app.downloadString.takeIf(String::isNotBlank)
    }
    val rating = app.labeledRating.takeUnless { it.isBlank() || it == "0.0" }

    Card(
        modifier = modifier
            .width(164.dp)
            .then(
                if (showHomeAction) {
                    Modifier.height(200.dp)
                } else {
                    Modifier.heightIn(min = 184.dp)
                }
            ),
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showHomeAction) {
                    AnimatedAppIcon(
                        modifier = Modifier.requiredSize(56.dp),
                        iconUrl = app.iconArtwork.url,
                        inProgress = isAnimated,
                        progress = if (status == DownloadStatus.DOWNLOADING) {
                            download?.progress?.toFloat() ?: 0F
                        } else {
                            0F
                        }
                    )
                } else {
                    AsyncImage(
                        modifier = Modifier
                            .requiredSize(56.dp)
                            .clip(RoundedCornerShape(dimensionResource(R.dimen.app_icon_radius))),
                        model = rememberStoreImageRequest(app.iconArtwork.url),
                        contentDescription = null,
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(Modifier.weight(1f))
                if (showHomeAction) {
                    FilledTonalButton(
                        onClick = onActionClick,
                        enabled = isActionEnabled,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = actionLabel?.let { stringResource(it) } ?: app.price,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }
                } else {
                    AppRatingChip(rating = app.labeledRating.ifBlank { "—" })
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = app.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.developerName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (showHomeAction && (installCount != null || rating != null)) {
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        dimensionResource(R.dimen.spacing_small)
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    rating?.let { AppRatingChip(rating = it) }
                    installCount?.let {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_download_manager),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else if (!showHomeAction && app.downloadString.isNotBlank()) {
                Text(
                    text = app.downloadString,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun AppListItemPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    AppListItem(app = app)
}
