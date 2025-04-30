/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composables.details

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalAsyncImagePreviewHandler
import coil3.compose.rememberAsyncImagePainter
import coil3.compose.rememberConstraintsSizeResolver
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.aurora.extensions.shimmer
import com.aurora.gplayapi.data.models.App
import com.aurora.store.compose.composables.preview.AppPreviewProvider
import com.aurora.store.compose.composables.preview.coilPreviewProvider

/**
 * Composable to display a screenshot of an app
 * @param modifier The modifier to be applied to the composable
 * @param url URL of the screenshot
 */
@Composable
fun ScreenshotComposable(modifier: Modifier = Modifier, url: String) {
    // See https://coil-kt.github.io/coil/compose/#rememberasyncimagepainter
    val sizeResolver = rememberConstraintsSizeResolver()
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .size(sizeResolver)
            .crossfade(true)
            .build()
    )
    val state by painter.state.collectAsStateWithLifecycle()
    val aspectRatioModifier = state.painter?.intrinsicSize?.let { intrinsicSize ->
        Modifier.aspectRatio(ratio = intrinsicSize.width / intrinsicSize.height)
    }

    Image(
        painter = painter,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .shimmer(state is AsyncImagePainter.State.Loading)
            .then(sizeResolver)
            .then(aspectRatioModifier ?: Modifier)
    )
}

@Preview
@Composable
@OptIn(ExperimentalCoilApi::class)
private fun ScreenshotComposablePreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    CompositionLocalProvider(LocalAsyncImagePreviewHandler provides coilPreviewProvider) {
        ScreenshotComposable(url = app.screenshots.firstOrNull()?.url ?: "")
    }
}
