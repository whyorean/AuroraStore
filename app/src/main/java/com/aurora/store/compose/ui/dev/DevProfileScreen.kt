/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.dev

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.aurora.extensions.adaptiveNavigationIcon
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.StreamBundle
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.data.models.details.DevStream
import com.aurora.store.R
import com.aurora.store.compose.composable.ContainedLoadingIndicator
import com.aurora.store.compose.composable.Placeholder
import com.aurora.store.compose.composable.StreamCarousel
import com.aurora.store.compose.composable.TopAppBar
import com.aurora.store.compose.composable.app.LargeAppListItem
import com.aurora.store.compose.navigation.Destination
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.ThemePreviewProvider
import com.aurora.store.data.model.ViewState
import com.aurora.store.viewmodel.details.DevProfileViewModel
import com.aurora.store.viewmodel.search.SearchViewModel
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Screen to display apps from a developer using the developerId
 * for e.g. https://play.google.com/store/apps/dev?id=5700313618786177705
 */
@Composable
fun DevProfileScreen(
    developerId: String,
    onNavigateTo: (Destination) -> Unit,
    viewModel: DevProfileViewModel = hiltViewModel()
) {
    val state by viewModel.liveData.observeAsState()

    LaunchedEffect(developerId) { viewModel.getStreamBundle(developerId) }

    val devStream = (state as? ViewState.Success<*>)?.data as? DevStream

    DevStreamContent(
        title = devStream?.title?.takeIf { it.isNotBlank() } ?: developerId,
        streamBundle = devStream?.streamBundle,
        isError = state is ViewState.Error,
        onRetry = { viewModel.getStreamBundle(developerId) },
        onClusterScrolled = viewModel::observeCluster,
        onNavigateTo = onNavigateTo
    )
}

@Composable
private fun DevStreamContent(
    title: String,
    streamBundle: StreamBundle?,
    isError: Boolean,
    onRetry: () -> Unit = {},
    onClusterScrolled: (StreamCluster) -> Unit = {},
    onNavigateTo: (Destination) -> Unit = {},
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfoV2()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = title,
                navigationIcon = windowAdaptiveInfo.adaptiveNavigationIcon
            )
        }
    ) { paddingValues ->
        if (isError) {
            Placeholder(
                modifier = Modifier.padding(paddingValues),
                painter = painterResource(R.drawable.ic_refresh),
                message = stringResource(R.string.error),
                actionLabel = stringResource(R.string.action_retry),
                onAction = onRetry
            )
        } else {
            // A null bundle renders the loading shimmer until the stream resolves
            StreamCarousel(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                streamBundle = streamBundle,
                onHeaderClick = { onNavigateTo(Destination.StreamBrowse(it)) },
                onAppClick = { onNavigateTo(Destination.AppDetails(it.packageName)) },
                onClusterScrolled = onClusterScrolled
            )
        }
    }
}

/**
 * Screen to display apps from a developer by searching using the publisherId
 * for e.g. https://play.google.com/store/apps/developer?id=The+Tor+Project
 */
@Composable
fun DevProfileScreen(
    publisherId: String,
    onNavigateTo: (Destination) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val apps = viewModel.apps.collectAsLazyPagingItems()

    LaunchedEffect(key1 = Unit) { viewModel.search("pub:$publisherId") }

    ScreenContent(
        apps = apps,
        topAppBarTitle = publisherId,
        onNavigateTo = onNavigateTo
    )
}

@Composable
private fun ScreenContent(
    apps: LazyPagingItems<App>,
    topAppBarTitle: String,
    onNavigateTo: (Destination) -> Unit = {},
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfoV2()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = topAppBarTitle,
                navigationIcon = windowAdaptiveInfo.adaptiveNavigationIcon
            )
        }
    ) { paddingValues ->
        when (apps.loadState.refresh) {
            is LoadState.Loading -> ContainedLoadingIndicator()

            is LoadState.Error -> {
                Placeholder(
                    modifier = Modifier.padding(paddingValues),
                    painter = painterResource(R.drawable.ic_refresh),
                    message = stringResource(R.string.error),
                    actionLabel = stringResource(R.string.action_retry),
                    onAction = { apps.retry() }
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                        .padding(vertical = dimensionResource(R.dimen.spacing_medium)),
                    verticalArrangement = Arrangement.spacedBy(
                        dimensionResource(R.dimen.spacing_medium)
                    )
                ) {
                    items(count = apps.itemCount, key = apps.itemKey { it.id }) { index ->
                        apps[index]?.let { app ->
                            LargeAppListItem(
                                app = app,
                                onClick = { onNavigateTo(Destination.AppDetails(app.packageName)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview
@Composable
private fun DevProfileScreenPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    val apps = List(10) { app.copy(id = Random.nextInt()) }
    val pagedApps = MutableStateFlow(PagingData.from(apps)).collectAsLazyPagingItems()

    ScreenContent(
        topAppBarTitle = app.developerName,
        apps = pagedApps
    )
}
