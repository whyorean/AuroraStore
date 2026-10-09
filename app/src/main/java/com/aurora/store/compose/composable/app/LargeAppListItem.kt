/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.aurora.store.data.model.DownloadStatus
import com.aurora.store.data.room.download.Download
import com.aurora.extensions.requiresGMS
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.compose.composable.app.AnimatedAppIcon
import com.aurora.store.compose.composable.auroraSegmentedListItemShapes
import com.aurora.store.compose.composable.rememberStoreImageRequest
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.ThemePreviewProvider
import com.aurora.store.util.CommonUtil
import com.aurora.store.util.PackageUtil
import coil3.compose.AsyncImage
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LargeAppListItem(
    modifier: Modifier = Modifier,
    app: App,
    itemIndex: Int = 0,
    itemCount: Int = 1,
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

    SegmentedListItem(
        selected = false,
        onClick = onClick,
        shapes = auroraSegmentedListItemShapes(itemIndex, itemCount),
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
            if (showHomeAction) {
                AnimatedAppIcon(
                    modifier = Modifier.requiredSize(dimensionResource(R.dimen.icon_size_medium)),
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
                        .requiredSize(dimensionResource(R.dimen.icon_size_medium))
                        .clip(RoundedCornerShape(dimensionResource(R.dimen.app_icon_radius))),
                    model = rememberStoreImageRequest(app.iconArtwork.url),
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
            }
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = app.developerName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (showHomeAction && (installCount != null || rating != null)) {
                    Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        installCount?.let {
                            AppInlineMetric(
                                icon = R.drawable.ic_download_manager,
                                value = it
                            )
                        }
                        rating?.let {
                            AppRatingChip(rating = it)
                        }
                    }
                }
                Text(
                    text = buildAppExtras(app),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        trailingContent = if (showHomeAction) {
            {
                FilledTonalButton(
                    onClick = onActionClick,
                    enabled = isActionEnabled,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = actionLabel?.let { stringResource(it) } ?: app.price,
                        maxLines = 1
                    )
                }
            }
        } else {
            null
        }
    ) {
        Text(
            text = app.displayName,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AppInlineMetric(icon: Int, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun buildAppExtras(app: App): String {
    val priceLabel = stringResource(if (app.isFree) R.string.details_free else R.string.details_paid)
    val adsLabel = stringResource(
        if (app.containsAds) R.string.details_contains_ads else R.string.details_no_ads
    )
    val gmsLabel = stringResource(R.string.details_gsf_dependent)
    val requiresGms = app.requiresGMS()

    return remember(
        app.size,
        app.isFree,
        app.containsAds,
        requiresGms,
        priceLabel,
        adsLabel,
        gmsLabel
    ) {
        buildList {
            if (app.size > 0) add(CommonUtil.addSiPrefix(app.size))
            add(priceLabel)
            add(adsLabel)
            if (requiresGms) add(gmsLabel)
        }.joinToString(separator = "  •  ")
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
fun LargeAppListItemPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    LargeAppListItem(app = app)
}
