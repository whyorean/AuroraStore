/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.dev

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.LocalAsyncImagePreviewHandler
import com.aurora.extensions.adaptiveNavigationIcon
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.compose.composables.TopAppBarComposable
import com.aurora.store.compose.composables.app.AppListComposable
import com.aurora.store.compose.composables.preview.AppPreviewProvider
import com.aurora.store.compose.composables.preview.coilPreviewProvider
import com.aurora.store.viewmodel.details.DevProfileViewModel
import com.aurora.store.viewmodel.search.SearchResultViewModel
import kotlinx.coroutines.flow.flowOf
import kotlin.random.Random

/**
 * Screen to display apps from a developer using the developerId
 * for e.g. https://play.google.com/store/apps/dev?id=5700313618786177705
 */
@Composable
fun DevProfileScreen(
    developerId: String,
    onNavigateUp: () -> Unit,
    onNavigateToAppDetails: (packageName: String) -> Unit,
    viewModel: DevProfileViewModel = hiltViewModel()
) {
    // TODO: Implement when migrating logic for current DevProfileFragment
}

/**
 * Screen to display apps from a developer by searching using the publisherId
 * for e.g. https://play.google.com/store/apps/developer?id=The+Tor+Project
 */
@Composable
fun DevProfileScreen(
    publisherId: String,
    onNavigateUp: () -> Unit,
    onNavigateToAppDetails: (packageName: String) -> Unit,
    viewModel: SearchResultViewModel = hiltViewModel()
) {
    val apps = viewModel.apps.collectAsLazyPagingItems()

    LaunchedEffect(key1 = Unit) { viewModel.search("pub:$publisherId") }

    ScreenContent(
        apps = apps,
        topAppBarTitle = publisherId,
        onNavigateUp = onNavigateUp,
        onNavigateToAppDetails = onNavigateToAppDetails
    )
}

@Composable
private fun ScreenContent(
    apps: LazyPagingItems<App>,
    topAppBarTitle: String,
    onNavigateUp: () -> Unit = {},
    onNavigateToAppDetails: (packageName: String) -> Unit = {},
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo()
) {
    Scaffold(
        topBar = {
            TopAppBarComposable(
                title = topAppBarTitle,
                navigationIcon = windowAdaptiveInfo.adaptiveNavigationIcon,
                onNavigateUp = onNavigateUp
            )
        }
    ) { paddingValues ->
        when (apps.loadState.refresh) {
            is LoadState.Loading -> {}

            is LoadState.Error -> {}

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                        .padding(vertical = dimensionResource(R.dimen.padding_medium))
                ) {
                    items(count = apps.itemCount, key = apps.itemKey { it.id }) { index ->
                        apps[index]?.let { app ->
                            AppListComposable(
                                app = app,
                                onClick = { onNavigateToAppDetails(app.packageName) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
@OptIn(ExperimentalCoilApi::class)
private fun DevProfileScreenPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    CompositionLocalProvider(LocalAsyncImagePreviewHandler provides coilPreviewProvider) {
        val apps = List(10) { app.copy(id = Random.nextInt()) }
        val pagedApps = flowOf(PagingData.from(apps)).collectAsLazyPagingItems()

        ScreenContent(
            topAppBarTitle = app.developerName,
            apps = pagedApps
        )
    }
}
