/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.updates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurora.store.R
import com.aurora.store.compose.composable.Placeholder
import com.aurora.store.compose.composable.SectionHeader
import com.aurora.store.compose.composable.ShimmerUpdateItem
import com.aurora.store.compose.composable.app.AppUpdateItem
import com.aurora.store.compose.navigation.Destination
import com.aurora.store.data.model.DownloadStatus
import com.aurora.store.data.room.download.Download
import com.aurora.store.data.room.update.Update
import com.aurora.store.viewmodel.all.UpdatesViewModel

@Composable
fun UpdatesScreen(
    viewModel: UpdatesViewModel = hiltViewModel(),
    onNavigateTo: (Destination) -> Unit = {},
    onRequestUpdate: (Update) -> Unit = {},
    onRequestUpdateAll: (List<Update>) -> Unit = {},
    onCancelUpdate: (String) -> Unit = {},
    onCancelAll: () -> Unit = {}
) {
    val context = LocalContext.current
    val updates by viewModel.updates.collectAsStateWithLifecycle()
    val ignoredUpdates by viewModel.ignoredUpdates.collectAsStateWithLifecycle()
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

    val groupedUpdates = remember(updateMap) {
        val main = mutableListOf<Map.Entry<Update, Download?>>()
        val approval = mutableListOf<Map.Entry<Update, Download?>>()
        val incompatible = mutableListOf<Map.Entry<Update, Download?>>()
        updateMap?.entries?.forEach { entry ->
            when {
                entry.key.isIncompatible -> incompatible += entry
                entry.key.requiresOwnershipTransfer(context) -> approval += entry
                else -> main += entry
            }
        }
        Triple(main.toList(), approval.toList(), incompatible.toList())
    }
    val (mainEntries, approvalEntries, incompatibleEntries) = groupedUpdates

    val mainAnyActive = mainEntries.any { it.value.isActive() }
    val approvalAnyActive = approvalEntries.any { it.value.isActive() }

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
                        dimensionResource(R.dimen.spacing_medium)
                    )
                ) {
                    items(10) { ShimmerUpdateItem() }
                }
            }

            updateMap.isEmpty() && ignoredUpdates.isEmpty() -> {
                Placeholder(
                    modifier = Modifier.fillMaxSize(),
                    painter = painterResource(R.drawable.ic_updates),
                    message = stringResource(R.string.details_no_updates),
                    actionLabel = stringResource(R.string.check_updates),
                    onAction = { viewModel.fetchUpdates() }
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(
                        dimensionResource(R.dimen.spacing_medium)
                    )
                ) {
                    if (mainEntries.isNotEmpty()) {
                        item(key = "header_main") {
                            val title = "${mainEntries.size} " + stringResource(
                                if (mainEntries.size == 1) {
                                    R.string.update_available
                                } else {
                                    R.string.updates_available
                                }
                            )
                            val actionLabel = stringResource(
                                if (mainAnyActive) {
                                    R.string.download_cancel_all
                                } else {
                                    R.string.action_update_all
                                }
                            )
                            SectionHeader(
                                title = title,
                                trailing = {
                                    TextButton(
                                        onClick = {
                                            if (mainAnyActive) {
                                                onCancelAll()
                                            } else {
                                                onRequestUpdateAll(mainEntries.map { it.key })
                                            }
                                        }
                                    ) { Text(actionLabel) }
                                }
                            )
                        }
                        updateItems(
                            entries = mainEntries,
                            keyPrefix = "main",
                            onNavigateTo = onNavigateTo,
                            onRequestUpdate = onRequestUpdate,
                            onCancelUpdate = onCancelUpdate
                        )
                    }

                    if (approvalEntries.isNotEmpty()) {
                        item(key = "header_approval") {
                            val actionLabel = stringResource(
                                if (approvalAnyActive) {
                                    R.string.download_cancel_all
                                } else {
                                    R.string.action_update_all
                                }
                            )
                            SectionHeader(
                                title = stringResource(R.string.updates_approval_header),
                                subtitle = stringResource(R.string.updates_approval_desc),
                                trailing = {
                                    TextButton(
                                        onClick = {
                                            if (approvalAnyActive) {
                                                onCancelAll()
                                            } else {
                                                onRequestUpdateAll(
                                                    approvalEntries.map { it.key }
                                                )
                                            }
                                        }
                                    ) { Text(actionLabel) }
                                }
                            )
                        }
                        updateItems(
                            entries = approvalEntries,
                            keyPrefix = "approval",
                            onNavigateTo = onNavigateTo,
                            onRequestUpdate = onRequestUpdate,
                            onCancelUpdate = onCancelUpdate
                        )
                    }

                    if (incompatibleEntries.isNotEmpty()) {
                        item(key = "header_incompatible") {
                            SectionHeader(
                                title = stringResource(R.string.updates_incompatible_header),
                                subtitle = stringResource(R.string.updates_incompatible_desc)
                            )
                        }
                        updateItems(
                            entries = incompatibleEntries,
                            keyPrefix = "incompatible",
                            onNavigateTo = onNavigateTo,
                            onRequestUpdate = onRequestUpdate,
                            onCancelUpdate = onCancelUpdate
                        )
                    }

                    if (ignoredUpdates.isNotEmpty()) {
                        item(key = "header_ignored") {
                            SectionHeader(
                                title = stringResource(R.string.updates_ignored_header),
                                subtitle = stringResource(R.string.updates_ignored_desc)
                            )
                        }
                        items(
                            items = ignoredUpdates,
                            key = { "ignored-${it.packageName}" }
                        ) { update ->
                            AppUpdateItem(
                                update = update,
                                onClick = {},
                                onUnignore = { viewModel.unignore(update.packageName) }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Download?.isActive(): Boolean {
    if (this == null) return false
    return !isFinished || status == DownloadStatus.COMPLETED
}

private fun LazyListScope.updateItems(
    entries: List<Map.Entry<Update, Download?>>,
    keyPrefix: String,
    onNavigateTo: (Destination) -> Unit,
    onRequestUpdate: (Update) -> Unit,
    onCancelUpdate: (String) -> Unit
) {
    items(
        items = entries,
        key = { "$keyPrefix-${it.key.packageName}" }
    ) { (update, download) ->
        AppUpdateItem(
            update = update,
            download = download,
            onClick = {
                onNavigateTo(Destination.AppUpdate(update))
            },
            onUpdate = { onRequestUpdate(update) },
            onCancel = { onCancelUpdate(update.packageName) }
        )
    }
}
