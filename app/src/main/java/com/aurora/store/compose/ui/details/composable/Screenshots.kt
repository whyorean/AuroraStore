/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.Artwork
import com.aurora.store.R
import com.aurora.store.compose.composable.details.ScreenshotListItem
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.ThemePreviewProvider
import kotlinx.coroutines.delay

/**
 * Composable to display screenshots of the app, supposed to be used as a part
 * of the Column with proper vertical arrangement spacing in the AppDetailsScreen.
 * @param screenshots Screenshots to display
 * @param onNavigateToScreenshot Callback when a screenshot is clicked
 */
@Composable
fun Screenshots(screenshots: List<Artwork>, onNavigateToScreenshot: (index: Int) -> Unit = {}) {
    // Play sometimes returns the same artwork URL twice, which would crash the LazyRow
    // with duplicate keys; deduping also keeps indices aligned with the ScreenshotScreen
    // pager which displays the same deduped list.
    val distinctScreenshots = remember(screenshots) {
        screenshots.distinctBy { it.url }
    }
    if (distinctScreenshots.isEmpty()) return

    var screenshotsReady by remember(distinctScreenshots) { mutableStateOf(false) }
    LaunchedEffect(distinctScreenshots) {
        delay(480)
        screenshotsReady = true
    }

    HorizontalMultiBrowseCarousel(
        state = rememberCarouselState { distinctScreenshots.size },
        modifier = Modifier
            .fillMaxWidth()
            .height(384.dp),
        preferredItemWidth = 216.dp,
        itemSpacing = 8.dp,
        contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.spacing_large))
    ) { index ->
        val artwork = distinctScreenshots[index]
        val screenshotModifier = Modifier
            .fillMaxSize()
            .maskClip(MaterialTheme.shapes.extraLarge)
            .clickable(onClick = { onNavigateToScreenshot(index) })
        var imageReady by remember(artwork.url, distinctScreenshots) {
            mutableStateOf(false)
        }
        LaunchedEffect(screenshotsReady, index) {
            if (screenshotsReady) {
                delay(index * 100L)
                imageReady = true
            }
        }
        if (imageReady) {
            ScreenshotListItem(
                modifier = screenshotModifier,
                url = "${artwork.url}=rw-w480-v1-e15",
                contentScale = ContentScale.Fit,
                crossfade = false,
                showLoadingShimmer = false
            )
        } else {
            Box(
                modifier = screenshotModifier.background(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.extraLarge
                )
            )
        }
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun ScreenshotsPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
    ) {
        Screenshots(screenshots = app.screenshots)
    }
}
