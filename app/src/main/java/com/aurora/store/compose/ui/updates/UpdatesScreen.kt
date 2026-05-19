/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.updates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurora.store.R
import com.aurora.store.compose.composable.EmptyState
import com.aurora.store.compose.composable.SectionHeaderWithAction
import com.aurora.store.compose.composable.ShimmerUpdateItem
import com.aurora.store.compose.composable.app.AppUpdateItem
import com.aurora.store.compose.navigation.Destination
import com.aurora.store.data.model.MinimalApp
import com.aurora.store.data.room.update.Update
import com.aurora.store.viewmodel.all.UpdatesViewModel

@Composable
fun UpdatesScreen(
    viewModel: UpdatesViewModel = hiltViewModel(),
    onNavigateTo: (Destination) -> Unit = {},
    onRequestUpdate: (Update) -> Unit = {},
    onRequestUpdateAll: () -> Unit = {},
    onCancelUpdate: (String) -> Unit = {},
    onCancelAll: () -> Unit = {}
) {
    val updates by viewModel.updates.collectAsStateWithLifecycle()
    val downloads by viewModel.downloadsList.collectAsStateWithLifecycle()
    val fetchingUpdates by viewModel.fetchingUpdates.collectAsStateWithLifecycle()

    val updateMap = remember(updates, downloads) {
        updates?.associateWith { update ->
            downloads.find {
                it.packageName == update.packageName &&
                    it.versionCode == update.versionCode
            }
        }
    }
    val allEnqueued = updateMap?.isNotEmpty() == true &&
        updateMap.values.all { it?.isRunning == true }

    PullToRefreshBox(
        modifier = Modifier.fillMaxSize(),
        isRefreshing = fetchingUpdates,
        onRefresh = { viewModel.fetchUpdates() }
    ) {
        when {
            updateMap == null -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(
                        dimensionResource(R.dimen.margin_medium)
                    )
                ) {
                    items(10) { ShimmerUpdateItem() }
                }
            }

            updateMap.isEmpty() -> {
                EmptyState(
                    modifier = Modifier.fillMaxSize(),
                    icon = R.drawable.ic_updates,
                    message = R.string.details_no_updates,
                    actionLabel = R.string.check_updates,
                    onAction = { viewModel.fetchUpdates() }
                )
            }

            else -> {
                val title = "${updateMap.size} " + stringResource(
                    if (updateMap.size == 1) {
                        R.string.update_available
                    } else {
                        R.string.updates_available
                    }
                )
                val actionLabel = stringResource(
                    if (allEnqueued) R.string.action_cancel else R.string.action_update_all
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(
                        dimensionResource(R.dimen.margin_medium)
                    )
                ) {
                    item(key = "header") {
                        SectionHeaderWithAction(
                            title = title,
                            action = actionLabel,
                            onAction = {
                                if (allEnqueued) onCancelAll() else onRequestUpdateAll()
                            }
                        )
                    }
                    items(
                        items = updateMap.entries.toList(),
                        key = { it.key.packageName }
                    ) { (update, download) ->
                        AppUpdateItem(
                            update = update,
                            download = download,
                            onClick = { onNavigateTo(Destination.AppDetails(update.packageName)) },
                            onLongClick = {
                                onNavigateTo(Destination.AppMenu(MinimalApp.fromUpdate(update)))
                            },
                            onUpdate = { onRequestUpdate(update) },
                            onCancel = { onCancelUpdate(update.packageName) }
                        )
                    }
                }
            }
        }
    }
}
