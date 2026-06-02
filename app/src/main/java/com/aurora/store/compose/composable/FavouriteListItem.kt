/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewWrapper
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.aurora.gplayapi.data.models.App
import com.aurora.store.AuroraApp
import com.aurora.store.R
import com.aurora.store.compose.composable.app.AnimatedAppIcon
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.ThemePreviewProvider
import com.aurora.store.compose.theme.colorGreen
import com.aurora.store.compose.theme.colorRed
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.data.model.DownloadStatus
import com.aurora.store.data.room.download.Download
import com.aurora.store.data.room.favourite.Favourite
import com.aurora.store.util.PackageUtil
import kotlinx.coroutines.flow.filter

@Composable
fun FavouriteListItem(
    modifier: Modifier = Modifier,
    favourite: Favourite,
    download: Download? = null,
    onClick: () -> Unit = {},
    onClear: () -> Unit = {}
) {
    val context = LocalContext.current

    // Seed from a one-shot check, then keep it live so the installed tick reflects the app
    // being installed or removed without leaving the screen.
    var isInstalled by remember(favourite.packageName) {
        mutableStateOf(PackageUtil.isInstalled(context, favourite.packageName))
    }
    LaunchedEffect(favourite.packageName) {
        AuroraApp.events.installerEvent
            .filter { it.packageName == favourite.packageName }
            .collect { event ->
                when (event) {
                    is InstallerEvent.Installed -> isInstalled = true
                    is InstallerEvent.Uninstalled -> isInstalled = false
                    else -> {}
                }
            }
    }

    // Show download/install progress on the row; only non-finished statuses count as in
    // progress, so a downloaded-but-not-installed (COMPLETED) app stays in its idle state.
    val inProgress = download != null && !download.isFinished
    val progress = if (download?.status == DownloadStatus.DOWNLOADING) {
        download.progress.toFloat()
    } else {
        0f
    }

    val statusText = when {
        download?.status == DownloadStatus.DOWNLOADING ->
            "${stringResource(R.string.status_downloading)} • ${download.progress}%"

        inProgress && download != null -> stringResource(download.status.localized)
        else -> DateUtils.formatDateTime(context, favourite.added, DateUtils.FORMAT_SHOW_DATE)
    }

    RemovableListItem(onRemove = onClear) { triggerRemove ->
        AuroraListItem(
            modifier = modifier,
            headline = favourite.displayName,
            supporting = favourite.packageName,
            tertiary = statusText,
            headlineStyle = MaterialTheme.typography.bodyMedium,
            onClick = onClick,
            leading = {
                if (inProgress) {
                    AnimatedAppIcon(
                        modifier = Modifier
                            .requiredSize(dimensionResource(R.dimen.icon_size_medium)),
                        iconUrl = favourite.iconURL,
                        inProgress = true,
                        progress = progress
                    )
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(favourite.iconURL)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .requiredSize(dimensionResource(R.dimen.icon_size_medium))
                            .clip(RoundedCornerShape(dimensionResource(R.dimen.radius_medium)))
                    )
                }
            },
            trailing = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(
                        dimensionResource(R.dimen.spacing_xsmall)
                    )
                ) {
                    if (isInstalled && !inProgress) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = stringResource(R.string.title_installed),
                            tint = colorGreen
                        )
                    }
                    IconButton(onClick = triggerRemove) {
                        Icon(
                            painter = painterResource(R.drawable.ic_favorite_checked),
                            contentDescription = stringResource(R.string.action_favourite),
                            tint = colorRed
                        )
                    }
                }
            }
        )
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun FavouriteListItemPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    FavouriteListItem(favourite = Favourite.fromApp(app, Favourite.Mode.MANUAL))
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun FavouriteListItemDownloadingPreview(
    @PreviewParameter(AppPreviewProvider::class) app: App
) {
    FavouriteListItem(
        favourite = Favourite.fromApp(app, Favourite.Mode.MANUAL),
        download = Download.fromApp(app).copy(
            status = DownloadStatus.DOWNLOADING,
            progress = 45
        )
    )
}
