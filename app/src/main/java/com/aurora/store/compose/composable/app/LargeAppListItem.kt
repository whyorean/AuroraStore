/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewWrapper
import coil3.compose.AsyncImage
import com.aurora.extensions.requiresGMS
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.compose.composable.rememberStoreImageRequest
import com.aurora.store.compose.composable.auroraSegmentedListItemShapes
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.ThemePreviewProvider
import com.aurora.store.util.CommonUtil

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LargeAppListItem(
    modifier: Modifier = Modifier,
    app: App,
    itemIndex: Int = 0,
    itemCount: Int = 1,
    onClick: () -> Unit = {}
) {
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
            AsyncImage(
                modifier = Modifier
                    .requiredSize(dimensionResource(R.dimen.icon_size_medium))
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.app_icon_radius))),
                model = rememberStoreImageRequest(app.iconArtwork.url),
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
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
                Text(
                    text = buildAppExtras(app),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
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
private fun buildAppExtras(app: App): String {
    val priceLabel = stringResource(if (app.isFree) R.string.details_free else R.string.details_paid)
    val adsLabel = stringResource(
        if (app.containsAds) R.string.details_contains_ads else R.string.details_no_ads
    )
    val gmsLabel = stringResource(R.string.details_gsf_dependent)
    val requiresGms = app.requiresGMS()

    return remember(
        app.size,
        app.downloadString,
        app.labeledRating,
        app.isFree,
        app.containsAds,
        requiresGms,
        priceLabel,
        adsLabel,
        gmsLabel
    ) {
        buildList {
            add(if (app.size > 0) CommonUtil.addSiPrefix(app.size) else app.downloadString)
            add("${app.labeledRating}★")
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
