/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.installed

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.aurora.extensions.emptyPagingItems
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.compose.composable.ContainedLoadingIndicator
import com.aurora.store.compose.composable.Error
import com.aurora.store.compose.composable.TopAppBar
import com.aurora.store.compose.composable.app.LargeAppListItem
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.PreviewTemplate
import com.aurora.store.viewmodel.all.InstalledViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.random.Random

@Composable
fun InstalledScreen(
    onNavigateUp: () -> Unit,
    onNavigateToAppDetails: (packageName: String) -> Unit,
    viewModel: InstalledViewModel = hiltViewModel()
) {
    val apps = viewModel.apps.collectAsLazyPagingItems()

    ScreenContent(
        apps = apps,
        onNavigateUp = onNavigateUp,
        onNavigateToAppDetails = onNavigateToAppDetails
    )
}

@Composable
private fun ScreenContent(
    onNavigateUp: () -> Unit = {},
    apps: LazyPagingItems<App> = emptyPagingItems(),
    onNavigateToAppDetails: (packageName: String) -> Unit = {}
) {
    /*
     * For some reason paging3 frequently out-of-nowhere invalidates the list which causes
     * the loading animation to play again even if the keys are same causing a glitching effect.
     *
     * Save the initial loading state to make sure we don't replay the loading animation again.
     */
    var initialLoad by rememberSaveable { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.title_apps_games),
                onNavigateUp = onNavigateUp
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            when {
                apps.loadState.refresh is LoadState.Loading && initialLoad -> {
                    ContainedLoadingIndicator()
                }

                else -> {
                    initialLoad = false

                    if (apps.itemCount == 0) {
                        Error(
                            modifier = Modifier.padding(paddingValues),
                            painter = painterResource(R.drawable.ic_apps_outage),
                            message = stringResource(R.string.no_apps_available)
                        )
                    } else {
                        LazyColumn {
                            items(
                                count = apps.itemCount,
                                key = apps.itemKey { it.packageName }
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
}

@Preview
@Composable
private fun InstalledScreenPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    PreviewTemplate {
        val apps = List(15) { app.copy(packageName = Random.nextInt().toString()) }
        val pagedApps = MutableStateFlow(PagingData.from(apps)).collectAsLazyPagingItems()
        ScreenContent(apps = pagedApps)
    }
}
