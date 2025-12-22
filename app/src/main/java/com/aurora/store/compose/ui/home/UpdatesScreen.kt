/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.aurora.store.compose.composable.UpdateListItem
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.PreviewTemplate
import com.aurora.store.data.room.update.Update
import com.aurora.store.data.room.update.UpdateWithDownload
import com.aurora.store.viewmodel.all.UpdatesViewModel
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun UpdatesScreen(
    onNavigateToAppDetails: (packageName: String) -> Unit,
    viewModel: UpdatesViewModel = hiltViewModel()
) {
    val hasOngoingUpdates by viewModel.hasOngoingUpdates.collectAsStateWithLifecycle()
    val isCheckingUpdates by viewModel.isCheckingUpdates.collectAsStateWithLifecycle()
    val updatesCount by viewModel.updatesCount.collectAsStateWithLifecycle()
    val updates = viewModel.updates.collectAsLazyPagingItems()

    ScreenContent(
        updates = updates,
        totalUpdatesCount = updatesCount,
        hasOngoingUpdates = hasOngoingUpdates,
        isCheckingUpdates = isCheckingUpdates,
        onCheckUpdates = { viewModel.fetchUpdates() },
        onUpdate = { update -> viewModel.download(update) },
        onUpdateAll = { viewModel.downloadAll() },
        onCancel = { packageName -> viewModel.cancelDownload(packageName) },
        onCancelAll = { viewModel.cancelAll() },
        onNavigateToAppDetails = onNavigateToAppDetails
    )
}

@Composable
private fun ScreenContent(
    hasOngoingUpdates: Boolean = false,
    totalUpdatesCount: Int = 0,
    updates: LazyPagingItems<UpdateWithDownload> = emptyPagingItems(),
    isCheckingUpdates: Boolean = false,
    onCheckUpdates: () -> Unit = {},
    onUpdate: (update: Update) -> Unit = {},
    onUpdateAll: () -> Unit = {},
    onCancel: (packageName: String) -> Unit = {},
    onCancelAll: () -> Unit = {},
    onNavigateToAppDetails: (packageName: String) -> Unit = {}
) {
    /*
     * For some reason paging3 frequently out-of-nowhere invalidates the list which causes
     * the loading animation to play again even if the keys are same causing a glitching effect.
     *
     * Save the initial loading state to make sure we don't replay the loading animation again.
     */
    var initialLoad by rememberSaveable { mutableStateOf(true) }

    val state = rememberPullToRefreshState()

    PullToRefreshBox(
        modifier = Modifier.fillMaxSize(),
        isRefreshing = isCheckingUpdates,
        onRefresh = onCheckUpdates,
        state = state,
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                state = state,
                isRefreshing = isCheckingUpdates,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    ) {
        when {
            updates.loadState.refresh is LoadState.Loading && initialLoad -> {
                ContainedLoadingIndicator()
            }

            else -> {
                initialLoad = false

                if (updates.itemCount == 0) {
                    Error(
                        painter = painterResource(R.drawable.ic_updates),
                        message = stringResource(R.string.details_no_updates),
                        actionMessage = stringResource(R.string.check_updates),
                        onAction = onCheckUpdates
                    )
                } else {
                    LazyColumn {
                        stickyHeader {
                            Surface(modifier = Modifier.fillMaxWidth()) {
                                Header(
                                    hasOngoingUpdates = hasOngoingUpdates,
                                    totalUpdatesCount = totalUpdatesCount,
                                    onUpdateAll = onUpdateAll,
                                    onCancelAll = onCancelAll
                                )
                            }
                        }

                        items(
                            count = updates.itemCount,
                            key = updates.itemKey { it.update.packageName }
                        ) { index ->
                            updates[index]?.let { updateWithDownload ->
                                val update = updateWithDownload.update
                                UpdateListItem(
                                    modifier = Modifier.animateItem(),
                                    update = update,
                                    download = updateWithDownload.download,
                                    onUpdate = { onUpdate(update) },
                                    onCancel = { onCancel(update.packageName) },
                                    onClick = { onNavigateToAppDetails(update.packageName) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(
    modifier: Modifier = Modifier,
    totalUpdatesCount: Int = 1,
    onUpdateAll: () -> Unit = {},
    onCancelAll: () -> Unit = {},
    hasOngoingUpdates: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.padding_medium),
                vertical = dimensionResource(R.dimen.padding_xsmall)
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = when (totalUpdatesCount) {
                1 -> "$totalUpdatesCount ${stringResource(R.string.update_available)}"
                else -> "$totalUpdatesCount ${stringResource(R.string.updates_available)}"
            },
            style = MaterialTheme.typography.titleMediumEmphasized,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Button(onClick = if (hasOngoingUpdates) onCancelAll else onUpdateAll) {
            Text(
                text = when {
                    hasOngoingUpdates -> stringResource(R.string.action_cancel)
                    else -> stringResource(R.string.action_update_all)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UpdatesScreenPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    val context = LocalContext.current
    PreviewTemplate {
        val updates = List(10) {
            UpdateWithDownload(
                update = Update.fromApp(context, app)
                    .copy(packageName = Random.nextInt().toString()),
                download = null
            )
        }
        val pagedUpdates = MutableStateFlow(PagingData.from(updates)).collectAsLazyPagingItems()
        ScreenContent(
            isCheckingUpdates = true,
            updates = pagedUpdates
        )
    }
}
