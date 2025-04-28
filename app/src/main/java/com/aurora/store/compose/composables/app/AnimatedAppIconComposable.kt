/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composables.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImage
import coil3.compose.LocalAsyncImagePreviewHandler
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.compose.composables.preview.AppPreviewProvider
import com.aurora.store.compose.composables.preview.coilPreviewProvider

/**
 * Composable to show icon for an app that can be animated to also show install progress
 * @param modifier Modifier to alter the composable
 * @param iconUrl URL of the app icon
 * @param progress Progress to show, for e.g. download or install
 * @param inProgress Whether to show indeterminate or determinate progress bar
 */
@Composable
fun AnimatedAppIconComposable(
    modifier: Modifier = Modifier,
    iconUrl: String,
    progress: Float = 0F,
    inProgress: Boolean = false
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (inProgress) 0.75F else 1F,
        animationSpec = tween(durationMillis = 300)
    )
    val clip = when {
        inProgress -> CircleShape
        else -> RoundedCornerShape(dimensionResource(R.dimen.radius_medium))
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (inProgress) {
            val indicatorModifier = Modifier.fillMaxSize()
            if (progress > 0) {
                CircularProgressIndicator(
                    modifier = indicatorModifier,
                    progress = { progress / 100 }
                )
            } else {
                CircularProgressIndicator(modifier = indicatorModifier)
            }
        }

        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(iconUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(scaleX = animatedScale, scaleY = animatedScale)
                .clip(clip)
        )
    }
}

private class ProgressProvider: PreviewParameterProvider<Float> {
    override val values: Sequence<Float>
        get() = sequenceOf(0F, 50F)
}

@Preview(showBackground = true)
@Composable
@OptIn(ExperimentalCoilApi::class)
private fun AnimatedAppIconPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    CompositionLocalProvider(LocalAsyncImagePreviewHandler provides coilPreviewProvider) {
        AnimatedAppIconComposable(
            modifier = Modifier.requiredSize(dimensionResource(R.dimen.icon_size_large)),
            iconUrl = app.iconArtwork.url
        )
    }
}

@Preview(showBackground = true)
@Composable
@OptIn(ExperimentalCoilApi::class)
private fun AnimatedAppIconPreview(@PreviewParameter(ProgressProvider::class) progress: Float) {
    CompositionLocalProvider(LocalAsyncImagePreviewHandler provides coilPreviewProvider) {
        AnimatedAppIconComposable(
            modifier = Modifier.requiredSize(dimensionResource(R.dimen.icon_size_large)),
            iconUrl = "",
            inProgress = true,
            progress = progress
        )
    }
}
