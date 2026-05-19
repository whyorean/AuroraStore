/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.commons

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aurora.gplayapi.data.models.StreamBundle
import com.aurora.store.R
import com.aurora.store.compose.composable.Placeholder
import com.aurora.store.compose.composable.StreamCarousel
import com.aurora.store.compose.composable.TopAppBar
import com.aurora.store.compose.composition.collectForced
import com.aurora.store.compose.navigation.Destination
import com.aurora.store.data.model.ViewState
import com.aurora.store.viewmodel.subcategory.CategoryStreamViewModel

@Composable
fun CategoryBrowseScreen(
    title: String,
    browseUrl: String,
    onNavigateTo: (Destination) -> Unit,
    viewModel: CategoryStreamViewModel = hiltViewModel(
        creationCallback = { factory: CategoryStreamViewModel.Factory ->
            factory.create(browseUrl)
        }
    )
) {
    val uiState by viewModel.viewState.collectForced(ViewState.Loading)

    Scaffold(
        topBar = { TopAppBar(title = title) }
    ) { paddingValues ->
        if (uiState is ViewState.Error) {
            Placeholder(
                modifier = Modifier.padding(paddingValues),
                painter = painterResource(R.drawable.ic_disclaimer),
                message = stringResource(R.string.error)
            )
        } else {
            val bundle = (uiState as? ViewState.Success<*>)?.data as? StreamBundle
            StreamCarousel(
                modifier = Modifier.padding(paddingValues),
                streamBundle = bundle,
                filterSingleAppClusters = false,
                onHeaderClick = { cluster ->
                    if (cluster.clusterBrowseUrl.isNotBlank()) {
                        onNavigateTo(Destination.StreamBrowse(cluster))
                    }
                },
                onAppClick = { onNavigateTo(Destination.AppDetails(it.packageName)) },
                onClusterScrolled = { viewModel.fetchNextCluster(it) },
                onScrolledToEnd = { viewModel.fetchNextPage() }
            )
        }
    }
}
