/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.downloads

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
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
import com.aurora.extensions.toast
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.compose.composable.ContainedLoadingIndicator
import com.aurora.store.compose.composable.DownloadListItem
import com.aurora.store.compose.composable.Error
import com.aurora.store.compose.composable.TopAppBar
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.PreviewTemplate
import com.aurora.store.compose.ui.downloads.menu.DownloadsMenu
import com.aurora.store.compose.ui.downloads.menu.MenuItem
import com.aurora.store.data.model.DownloadStatus
import com.aurora.store.data.room.download.Download
import com.aurora.store.viewmodel.downloads.DownloadsViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.random.Random

@Composable
fun DownloadsScreen(
    onNavigateUp: () -> Unit,
    viewModel: DownloadsViewModel = hiltViewModel(),
    onNavigateToAppDetails: (packageName: String) -> Unit
) {
    val context = LocalContext.current
    val downloads = viewModel.downloads.collectAsLazyPagingItems()

    val exportMimeType = "application/zip"
    var requestedExport by rememberSaveable { mutableStateOf<Download?>(null) }
    val documentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(exportMimeType),
        onResult = {
            if (it != null) {
                requestedExport?.let { download -> viewModel.export(download, it) }
            } else {
                context.toast(R.string.failed_apk_export)
            }
            requestedExport = null
        }
    )

    ScreenContent(
        onNavigateUp = onNavigateUp,
        downloads = downloads,
        onNavigateToAppDetails = onNavigateToAppDetails,
        onCancelAll = { viewModel.cancelAll() },
        onForceClearAll = { viewModel.clearAll() },
        onClearFinished = { viewModel.clearFinished() },
        onCancel = { packageName -> viewModel.cancel(packageName) },
        onInstall = { download -> viewModel.install(download) },
        onClear = { download ->
            viewModel.clear(download.packageName, download.versionCode)
        },
        onExport = { download ->
            requestedExport = download
            documentLauncher.launch("${download.packageName}.zip")
        }
    )
}

@Composable
private fun ScreenContent(
    downloads: LazyPagingItems<Download> = emptyPagingItems(),
    onNavigateUp: () -> Unit = {},
    onNavigateToAppDetails: (packageName: String) -> Unit = {},
    onCancel: (packageName: String) -> Unit = {},
    onClear: (download: Download) -> Unit = {},
    onExport: (download: Download) -> Unit = {},
    onInstall: (download: Download) -> Unit = {},
    onCancelAll: () -> Unit = {},
    onForceClearAll: () -> Unit = {},
    onClearFinished: () -> Unit = {}
) {
    /*
     * For some reason paging3 frequently out-of-nowhere invalidates the list which causes
     * the loading animation to play again even if the keys are same causing a glitching effect.
     *
     * Save the initial loading state to make sure we don't replay the loading animation again.
     */
    var initialLoad by rememberSaveable { mutableStateOf(true) }

    @Composable
    fun SetupMenu() {
        DownloadsMenu { menuItem ->
            when (menuItem) {
                MenuItem.CANCEL_ALL -> onCancelAll()
                MenuItem.FORCE_CLEAR_ALL -> onForceClearAll()
                MenuItem.CLEAR_FINISHED -> onClearFinished()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.title_download_manager),
                onNavigateUp = onNavigateUp,
                actions = { if (downloads.itemCount != 0) SetupMenu() }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(vertical = dimensionResource(R.dimen.padding_medium))
        ) {
            when {
                downloads.loadState.refresh is LoadState.Loading && initialLoad -> {
                    ContainedLoadingIndicator()
                }

                else -> {
                    initialLoad = false

                    if (downloads.itemCount == 0) {
                        Error(
                            modifier = Modifier.padding(paddingValues),
                            painter = painterResource(R.drawable.ic_download_manager),
                            message = stringResource(R.string.download_none)
                        )
                    } else {
                        LazyColumn {
                            items(
                                count = downloads.itemCount,
                                key = downloads.itemKey { it.packageName }
                            ) { index ->
                                downloads[index]?.let { download ->
                                    DownloadListItem(
                                        modifier = Modifier.animateItem(),
                                        download = download,
                                        onClick = { onNavigateToAppDetails(download.packageName) },
                                        onClear = { onClear(download) },
                                        onCancel = { onCancel(download.packageName) },
                                        onExport = { onExport(download) },
                                        onInstall = { onInstall(download) }
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
private fun DownloadsScreenPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    PreviewTemplate {
        val downloads = List(10) {
            Download.fromApp(app).copy(
                packageName = Random.nextInt().toString(),
                status = DownloadStatus.entries.random()
            )
        }
        val pagedDownloads = MutableStateFlow(PagingData.from(downloads)).collectAsLazyPagingItems()
        ScreenContent(downloads = pagedDownloads)
    }
}
