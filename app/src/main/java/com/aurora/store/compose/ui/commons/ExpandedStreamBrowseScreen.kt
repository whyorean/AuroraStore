/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.commons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.aurora.store.R
import com.aurora.store.compose.composable.ContainedLoadingIndicator
import com.aurora.store.compose.composable.Error
import com.aurora.store.compose.composable.TopAppBar
import com.aurora.store.compose.composable.app.LargeAppListItem
import com.aurora.store.compose.navigation.Destination
import com.aurora.store.viewmodel.browse.ExpandedStreamBrowseViewModel
import kotlin.uuid.Uuid

@Composable
fun ExpandedStreamBrowseScreen(
    browseUrl: String,
    defaultTitle: String,
    onNavigateTo: (Destination) -> Unit,
    viewModel: ExpandedStreamBrowseViewModel = hiltViewModel(
        creationCallback = { factory: ExpandedStreamBrowseViewModel.Factory ->
            factory.create(browseUrl)
        }
    )
) {
    val title by viewModel.title.collectAsStateWithLifecycle()
    val apps = viewModel.apps.collectAsLazyPagingItems()

    Scaffold(
        topBar = {
            TopAppBar(
                title = title.ifBlank { defaultTitle }
            )
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
                            .padding(paddingValues),
                        verticalArrangement = Arrangement.spacedBy(
                            dimensionResource(R.dimen.margin_medium)
                        )
                    ) {
                        items(
                            count = apps.itemCount,
                            key = { Uuid.random().toString() }
                        ) { index ->
                            apps[index]?.let { app ->
                                LargeAppListItem(
                                    app = app,
                                    onClick = {
                                        onNavigateTo(Destination.AppDetails(app.packageName))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
