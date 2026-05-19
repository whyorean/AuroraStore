/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable.app

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
import com.aurora.extensions.requiresGMS
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.compose.composable.AuroraListItem
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.ThemePreviewProvider
import com.aurora.store.util.CommonUtil

@Composable
fun LargeAppListItem(modifier: Modifier = Modifier, app: App, onClick: () -> Unit = {}) {
    AuroraListItem(
        modifier = modifier,
        headline = app.displayName,
        supporting = app.developerName,
        tertiary = buildAppExtras(app),
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
private fun buildAppExtras(app: App): String = buildList {
    add(if (app.size > 0) CommonUtil.addSiPrefix(app.size) else app.downloadString)
    add("${app.labeledRating}★")
    add(stringResource(if (app.isFree) R.string.details_free else R.string.details_paid))
    add(
        stringResource(
            if (app.containsAds) R.string.details_contains_ads else R.string.details_no_ads
        )
    )
    if (app.requiresGMS()) add(stringResource(R.string.details_gsf_dependent))
}.joinToString(separator = "  •  ")

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
fun LargeAppListItemPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    LargeAppListItem(app = app)
}
