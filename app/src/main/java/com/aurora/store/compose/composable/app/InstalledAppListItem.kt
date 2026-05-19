/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable.app

import android.text.format.DateUtils
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewWrapper
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.compose.composable.AuroraListItem
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.ThemePreviewProvider
import com.aurora.store.util.CommonUtil

/**
 * List item for the installed-apps screen. Mirrors [LargeAppListItem] but the tertiary
 * line shows installation-relevant info (APK size, last update, installer) rather than
 * download / rating / price / ads which only matter on Play listings.
 *
 * Each tertiary segment is omitted if the corresponding value is missing, so the row
 * gracefully collapses when only some metadata is available.
 */
@Composable
fun InstalledAppListItem(
    modifier: Modifier = Modifier,
    app: App,
    sizeBytes: Long = 0L,
    lastUpdateTime: Long = 0L,
    installerLabel: String? = null,
    onClick: () -> Unit = {}
) {
    AuroraListItem(
        modifier = modifier,
        headline = app.displayName,
        supporting = app.packageName,
        tertiary = buildInstalledExtras(sizeBytes, lastUpdateTime, installerLabel),
        headlineStyle = MaterialTheme.typography.bodyMedium,
        onClick = onClick,
        leading = {
            AsyncImage(
                modifier = Modifier
                    .requiredSize(dimensionResource(R.dimen.icon_size_medium))
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.radius_medium))),
                model = ImageRequest.Builder(LocalContext.current)
                    .data(app.iconArtwork.url)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
        }
    )
}

@Composable
private fun buildInstalledExtras(
    sizeBytes: Long,
    lastUpdateTime: Long,
    installerLabel: String?
): String = buildList {
    if (sizeBytes > 0) add(CommonUtil.addSiPrefix(sizeBytes))
    if (lastUpdateTime > 0) {
        val relative = DateUtils.getRelativeTimeSpanString(
            lastUpdateTime,
            System.currentTimeMillis(),
            DateUtils.DAY_IN_MILLIS
        )
        add(stringResource(R.string.installed_updated_at, relative))
    }
    if (!installerLabel.isNullOrBlank()) add(installerLabel)
}.joinToString(separator = "  •  ")

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun InstalledAppListItemPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    InstalledAppListItem(
        app = app,
        sizeBytes = 54_300_000L,
        lastUpdateTime = System.currentTimeMillis() - DateUtils.DAY_IN_MILLIS * 5,
        installerLabel = "Aurora Store"
    )
}
