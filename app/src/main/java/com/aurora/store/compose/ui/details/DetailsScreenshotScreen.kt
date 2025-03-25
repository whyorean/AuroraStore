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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.aurora.store.R
import com.aurora.store.compose.composables.TopAppBarComposable
import com.aurora.store.viewmodel.details.AppDetailsViewModel

@Composable
fun DetailsScreenshotScreen(
    onNavigateUp: () -> Unit,
    viewModel: AppDetailsViewModel = hiltViewModel()
) {
    val app by viewModel.app.collectAsStateWithLifecycle(null)

    ScreenContent(
        topAppBarTitle = app!!.displayName,
        onNavigateUp = onNavigateUp,
        screenshots = app?.screenshots?.map { it.url } ?: emptyList()
    )
}

@Composable
private fun ScreenContent(
    topAppBarTitle: String? = null,
    onNavigateUp: () -> Unit = {},
    screenshots: List<String> = emptyList()
) {
    val displayMetrics = LocalContext.current.resources.displayMetrics
    val pagerState = rememberPagerState { screenshots.size }

    Scaffold(
        topBar = {
            TopAppBarComposable(title = topAppBarTitle, onNavigateUp = onNavigateUp)
        }
    ) { paddingValues ->
        HorizontalPager(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            state = pagerState
        ) {
            val screenshot = screenshots[pagerState.currentPage]
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data("$screenshot=rw-w${displayMetrics.widthPixels}-v1-e15")
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                placeholder = painterResource(R.drawable.bg_placeholder),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Preview
@Composable
private fun DetailsScreenshotScreenPreview() {
    ScreenContent(
        topAppBarTitle = stringResource(R.string.app_name)
    )
}
