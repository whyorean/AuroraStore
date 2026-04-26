/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.commons

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import com.aurora.extensions.emptyPagingItems
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.store.R
import com.aurora.store.compose.composable.ContainedLoadingIndicator
import com.aurora.store.compose.composable.Error
import com.aurora.store.compose.composable.TopAppBar
import com.aurora.store.compose.composable.app.LargeAppListItem
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.ThemePreviewProvider
import com.aurora.store.viewmodel.browse.StreamBrowseViewModel
import kotlin.random.Random
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun StreamBrowseScreen(
    streamCluster: StreamCluster,
    onNavigateUp: () -> Unit,
    onNavigateToAppDetails: (packageName: String) -> Unit,
    viewModel: StreamBrowseViewModel = hiltViewModel(
        creationCallback = { factory: StreamBrowseViewModel.Factory ->
            factory.create(streamCluster)
        }
    )
) {
    val apps = viewModel.apps.collectAsLazyPagingItems()

    ScreenContent(
        title = streamCluster.clusterTitle,
        apps = apps,
        onNavigateUp = onNavigateUp,
        onNavigateToAppDetails = onNavigateToAppDetails
    )
}

@Composable
private fun ScreenContent(
    title: String = String(),
    apps: LazyPagingItems<App> = emptyPagingItems(),
    onNavigateToAppDetails: (packageName: String) -> Unit = {},
    onNavigateUp: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(title = title, onNavigateUp = onNavigateUp)
        }
    ) { paddingValues ->
        when (apps.loadState.refresh) {
            is LoadState.Loading -> ContainedLoadingIndicator()

            is LoadState.Error -> {
                Error(
                    modifier = Modifier.padding(paddingValues),
                    painter = painterResource(R.drawable.ic_disclaimer),
                    message = stringResource(R.string.error)
                )
            }

            else -> {
                if (apps.itemCount == 0) {
                    Error(
                        modifier = Modifier.padding(paddingValues),
                        painter = painterResource(R.drawable.ic_disclaimer),
                        message = stringResource(R.string.no_apps_available)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        items(
                            count = apps.itemCount,
                            key = { Uuid.random().toString() }
                        ) { index ->
                            apps[index]?.let { app ->
                                LargeAppListItem(
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
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview
@Composable
private fun StreamBrowseScreenPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    val apps = List(10) { app.copy(id = Random.nextInt()) }
    val pagedApps = MutableStateFlow(PagingData.from(apps)).collectAsLazyPagingItems()
    ScreenContent(
        apps = pagedApps
    )
}
