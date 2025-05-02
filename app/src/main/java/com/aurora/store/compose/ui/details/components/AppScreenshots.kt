/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.LocalAsyncImagePreviewHandler
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.Artwork
import com.aurora.store.R
import com.aurora.store.compose.composables.details.ScreenshotComposable
import com.aurora.store.compose.composables.preview.AppPreviewProvider
import com.aurora.store.compose.composables.preview.coilPreviewProvider

/**
 * Composable to display screenshots of the app, supposed to be used as a part
 * of the Column with proper vertical arrangement spacing in the AppDetailsScreen.
 * @param screenshots Screenshots to display
 * @param onNavigateToScreenshot Callback when a screenshot is clicked
 */
@Composable
fun AppScreenshots(screenshots: List<Artwork>, onNavigateToScreenshot: (index: Int) -> Unit = {}) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_small))) {
        items(items = screenshots, key = { artwork -> artwork.url }) { artwork ->
            ScreenshotComposable(
                modifier = Modifier
                    .height(dimensionResource(R.dimen.screenshot_height))
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.radius_small)))
                    .clickable { onNavigateToScreenshot(screenshots.indexOf(artwork)) },
                url = "${artwork.url}=rw-w480-v1-e15"
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
@OptIn(ExperimentalCoilApi::class)
private fun AppScreenshotsPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_medium))) {
        CompositionLocalProvider(LocalAsyncImagePreviewHandler provides coilPreviewProvider) {
            AppScreenshots(screenshots = app.screenshots)
        }
    }
}
