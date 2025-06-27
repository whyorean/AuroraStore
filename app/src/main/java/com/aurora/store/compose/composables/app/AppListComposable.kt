/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composables.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImage
import coil3.compose.LocalAsyncImagePreviewHandler
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.aurora.extensions.bodyVerySmall
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.coilPreviewProvider
import com.aurora.store.util.CommonUtil
import com.aurora.store.util.PackageUtil

/**
 * Composable for displaying minimal app details in a vertical-scrollable list
 * @param modifier The modifier to be applied to the composable
 * @param app [App] to display
 * @param onClick Callback when the composable is clicked
 * @see AppComposable
 */
@Composable
fun AppListComposable(modifier: Modifier = Modifier, app: App, onClick: () -> Unit = {}) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = dimensionResource(R.dimen.padding_medium),
                vertical = dimensionResource(R.dimen.padding_small)
            )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(app.iconArtwork.url)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .requiredSize(dimensionResource(R.dimen.icon_size_medium))
                .clip(RoundedCornerShape(dimensionResource(R.dimen.radius_medium)))
        )
        Column(
            modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.margin_small)),
        ) {
            Text(
                text = app.displayName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(text = app.developerName, style = MaterialTheme.typography.bodySmall)
            Text(
                text = buildExtras(app).joinToString(separator = "  •  "),
                style = MaterialTheme.typography.bodyVerySmall
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
@OptIn(ExperimentalCoilApi::class)
fun AppListComposablePreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    CompositionLocalProvider(LocalAsyncImagePreviewHandler provides coilPreviewProvider) {
        AppListComposable(app = app)
    }
}

@Composable
private fun buildExtras(app: App): List<String> {
    return mutableListOf<String>().apply {
        add(if (app.size > 0) CommonUtil.addSiPrefix(app.size) else app.downloadString)
        add("${app.labeledRating}★")

        if (app.isFree) {
            add(stringResource(R.string.details_free))
        } else {
            add(stringResource(R.string.details_paid))
        }

        if (app.containsAds) {
            add(stringResource(R.string.details_contains_ads))
        } else {
            add(stringResource(R.string.details_no_ads))
        }

        if (app.dependencies.dependentPackages.contains(PackageUtil.PACKAGE_NAME_GMS)) {
            add(stringResource(R.string.details_gsf_dependent))
        }
    }
}
