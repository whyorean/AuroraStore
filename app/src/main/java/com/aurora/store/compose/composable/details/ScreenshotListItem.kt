/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable.details

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.compose.rememberConstraintsSizeResolver
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.aurora.extensions.shimmer
import com.aurora.gplayapi.data.models.App
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.ThemePreviewProvider

/**
 * Composable to display a screenshot of an app
 * @param modifier The modifier to be applied to the composable
 * @param url URL of the screenshot
 */
@Composable
fun ScreenshotListItem(
    modifier: Modifier = Modifier,
    url: String,
    contentScale: ContentScale = ContentScale.Fit,
    crossfade: Boolean = true,
    showLoadingShimmer: Boolean = true
) {
    // See https://coil-kt.github.io/coil/compose/#rememberasyncimagepainter
    val sizeResolver = rememberConstraintsSizeResolver()
    val context = LocalContext.current
    val request = remember(context, url, sizeResolver, crossfade) {
        ImageRequest.Builder(context)
            .data(url)
            .size(sizeResolver)
            .let { builder -> if (crossfade) builder.crossfade(true) else builder }
            .build()
    }
    val painter = rememberAsyncImagePainter(model = request)
    val isLoading = if (showLoadingShimmer) {
        painter.state.collectAsStateWithLifecycle().value is AsyncImagePainter.State.Loading
    } else {
        false
    }
    Image(
        painter = painter,
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier
            .then(
                if (showLoadingShimmer) {
                    Modifier.shimmer(isLoading)
                } else {
                    Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)
                }
            )
            .then(sizeResolver)
    )
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview
@Composable
private fun ScreenshotListItemPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    ScreenshotListItem(url = app.screenshots.firstOrNull()?.url ?: "")
}
