/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.installed

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.aurora.extensions.emptyPagingItems
import com.aurora.gplayapi.model.App
import com.aurora.store.R
import com.aurora.store.compose.composable.ContainedLoadingIndicator
import com.aurora.store.compose.composable.Placeholder
import com.aurora.store.compose.composable.ScrollHint
import com.aurora.store.compose.composable.TopAppBar
import com.aurora.store.compose.composable.app.InstalledAppListItem
import com.aurora.store.compose.navigation.Destination
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.ThemePreviewProvider
import com.aurora.store.compose.ui.commons.InstalledAppMeta
import com.aurora.store.compose.ui.commons.SortFilterSheet
import com.aurora.store.compose.ui.commons.SortFilterState
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.viewmodel.all.InstalledViewModel
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun InstalledScreen(
    onNavigateTo: (Destination) -> Unit,
    viewModel: InstalledViewModel = hiltViewModel()
) {
    val apps = viewModel.apps.collectAsLazyPagingItems()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val installers by viewModel.installers.collectAsStateWithLifecycle()
    val metadata by viewModel.metadata.collectAsStateWithLifecycle()

    ScreenContent(
        apps = apps,
        state = state,
        installers = installers,
        metadata = metadata,
        onStateChange = viewModel::updateState,
        onFavourite = viewModel::favourite,
        onUnfavourite = viewModel::unfavourite,
        onNavigateTo = onNavigateTo
    )
}

@Composable
private fun ScreenContent(
    apps: LazyPagingItems<App> = emptyPagingItems(),
    state: SortFilterState = SortFilterState(),
    installers: Map<String, String> = emptyMap(),
    metadata: Map<String, InstalledAppMeta> = emptyMap(),
    onStateChange: (SortFilterState) -> Unit = {},
    onFavourite: (List<App>) -> Unit = {},
    onUnfavourite: (Collection<String>) -> Unit = {},
    onNavigateTo: (Destination) -> Unit = {}
) {
    val context = LocalContext.current
    val selected = rememberSaveable(
        saver = listSaver(save = { it.toList() }, restore = { it.toMutableStateList() })
    ) { mutableStateListOf<String>() }

    BackHandler(enabled = selected.isNotEmpty()) { selected.clear() }

    fun toggle(app: App) {
        if (!selected.remove(app.packageName)) selected.add(app.packageName)
    }

    /*
     * For some reason paging3 frequently out-of-nowhere invalidates the list which causes
     * the loading animation to play again even if the keys are same causing a glitching effect.
     *
     * Save the initial loading state to make sure we don't replay the loading animation again.
     */
    var initialLoad by rememberSaveable { mutableStateOf(true) }
    var sheetVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (selected.isEmpty()) {
                TopAppBar(
                    title = stringResource(R.string.title_apps_games),
                    actions = {
                        IconButton(onClick = { sheetVisible = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_tune),
                                contentDescription = stringResource(R.string.installed_sort_filter)
                            )
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = pluralStringResource(
                        R.plurals.installed_selected,
                        selected.size,
                        selected.size
                    ),
                    navigationIcon = painterResource(R.drawable.ic_cancel),
                    actions = {
                        IconButton(
                            onClick = {
                                onFavourite(
                                    apps.itemSnapshotList.items
                                        .filter { it.packageName in selected }
                                )
                                selected.clear()
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_favorite_checked),
                                contentDescription = stringResource(R.string.action_favourite)
                            )
                        }
                        IconButton(
                            onClick = {
                                onUnfavourite(selected.toList())
                                selected.clear()
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_favorite_unchecked),
                                contentDescription = stringResource(R.string.action_unfavourite)
                            )
                        }
                        IconButton(
                            onClick = {
                                selected.forEach { AppInstaller.uninstall(context, it) }
                                selected.clear()
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete),
                                contentDescription = stringResource(R.string.action_uninstall)
                            )
                        }
                    }
                )
            }
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
                        Placeholder(
                            modifier = Modifier.padding(paddingValues),
                            painter = painterResource(R.drawable.ic_apps_outage),
                            message = stringResource(R.string.no_apps_available)
                        )
                    } else {
                        val listState = rememberLazyListState()
                        Box(modifier = Modifier.fillMaxSize()) {
                            LazyColumn(state = listState) {
                                items(
                                    count = apps.itemCount,
                                    key = apps.itemKey { it.packageName }
                                ) { index ->
                                    apps[index]?.let { app ->
                                        val meta = metadata[app.packageName]
                                        InstalledAppListItem(
                                            app = app,
                                            sizeBytes = meta?.sizeBytes ?: 0L,
                                            lastUpdateTime = meta?.lastUpdateTime ?: 0L,
                                            installerLabel = meta?.installer
                                                ?.let { installers[it] },
                                            selected = app.packageName in selected,
                                            onClick = {
                                                if (selected.isNotEmpty()) {
                                                    toggle(app)
                                                } else {
                                                    onNavigateTo(
                                                        Destination.AppDetails(app.packageName)
                                                    )
                                                }
                                            },
                                            onLongClick = { toggle(app) }
                                        )
                                    }
                                }
                            }
                            ScrollHint(
                                listState = listState,
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }
                    }
                }
            }
        }
    }

    if (sheetVisible) {
        SortFilterSheet(
            state = state,
            installers = installers,
            onStateChange = onStateChange,
            onDismiss = { sheetVisible = false }
        )
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview
@Composable
private fun InstalledScreenPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    val apps = List(15) { app.copy(packageName = Random.nextInt().toString()) }
    val pagedApps = MutableStateFlow(PagingData.from(apps)).collectAsLazyPagingItems()
    ScreenContent(apps = pagedApps)
}
