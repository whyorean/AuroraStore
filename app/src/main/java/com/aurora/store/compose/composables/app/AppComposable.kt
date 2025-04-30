/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composables.app

import android.text.format.Formatter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
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
import com.aurora.store.compose.composables.preview.AppPreviewProvider
import com.aurora.store.compose.composables.preview.coilPreviewProvider

/**
 * Composable for displaying minimal app details in a horizontal-scrollable list
 * @param modifier The modifier to be applied to the composable
 * @param app [App] to display
 * @param onClick Callback when the composable is clicked
 * @see AppListComposable
 */
@Composable
fun AppComposable(modifier: Modifier = Modifier, app: App, onClick: () -> Unit = {}) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .width(dimensionResource(R.dimen.icon_size_cluster))
            .clickable(onClick = onClick)
            .padding(dimensionResource(R.dimen.padding_xsmall))
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(app.iconArtwork.url)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .requiredSize(dimensionResource(R.dimen.icon_size_cluster))
                .clip(RoundedCornerShape(dimensionResource(R.dimen.radius_medium)))
        )
        Text(
            text = app.displayName,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = if (app.size > 0) {
                Formatter.formatShortFileSize(context, app.size)
            } else {
                app.downloadString
            },
            style = MaterialTheme.typography.bodyVerySmall
        )
    }
}

@Preview(showBackground = true)
@Composable
@OptIn(ExperimentalCoilApi::class)
private fun AppComposablePreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    CompositionLocalProvider(LocalAsyncImagePreviewHandler provides coilPreviewProvider) {
        AppComposable(app = app)
    }
}
