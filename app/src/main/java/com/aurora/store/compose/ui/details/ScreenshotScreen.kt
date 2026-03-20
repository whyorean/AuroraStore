/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurora.extensions.adaptiveNavigationIcon
import com.aurora.extensions.isWindowCompact
import com.aurora.gplayapi.data.models.Artwork
import com.aurora.store.R
import com.aurora.store.compose.composable.TopAppBar
import com.aurora.store.compose.composable.details.ScreenshotListItem
import com.aurora.store.compose.preview.PreviewTemplate
import com.aurora.store.viewmodel.details.AppDetailsViewModel

@Composable
fun ScreenshotScreen(
    packageName: String,
    index: Int,
    onNavigateUp: () -> Unit,
    viewModel: AppDetailsViewModel = hiltViewModel(key = packageName),
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo()
) {
    val app by viewModel.app.collectAsStateWithLifecycle()

    val topAppBarTitle = when {
        windowAdaptiveInfo.isWindowCompact -> app!!.displayName
        else -> stringResource(R.string.details_more_about_app)
    }

    ScreenContent(
        topAppBarTitle = topAppBarTitle,
        onNavigateUp = onNavigateUp,
        screenshots = app!!.screenshots,
        index = index
    )
}

@Composable
private fun ScreenContent(
    topAppBarTitle: String? = null,
    onNavigateUp: () -> Unit = {},
    screenshots: List<Artwork> = emptyList(),
    index: Int = 0,
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo()
) {
    val displayMetrics = LocalContext.current.resources.displayMetrics
    val pagerState = rememberPagerState(initialPage = index) { screenshots.size }

    LaunchedEffect(key1 = index) {
        if (pagerState.currentPage != index) pagerState.scrollToPage(index)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = topAppBarTitle,
                navigationIcon = windowAdaptiveInfo.adaptiveNavigationIcon,
                onNavigateUp = onNavigateUp
            )
        }
    ) { paddingValues ->
        HorizontalPager(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            state = pagerState
        ) { page ->
            val artwork = screenshots[page]
            ScreenshotListItem(
                modifier = Modifier.fillMaxSize(),
                url = "${artwork.url}=rw-w${displayMetrics.widthPixels}-v1-e15"
            )
        }
    }
}

@Preview
@Composable
private fun ScreenshotScreenPreview() {
    PreviewTemplate {
        ScreenContent(
            topAppBarTitle = stringResource(R.string.app_name)
        )
    }
}
