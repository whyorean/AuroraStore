/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.spoof

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.util.fastForEachIndexed
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aurora.Constants
import com.aurora.extensions.toast
import com.aurora.store.R
import com.aurora.store.compose.composable.TopAppBar
import com.aurora.store.compose.ui.spoof.menu.MenuItem
import com.aurora.store.compose.ui.spoof.menu.SpoofMenu
import com.aurora.store.compose.ui.spoof.navigation.SpoofPage
import com.aurora.store.data.providers.AccountProvider
import com.aurora.store.viewmodel.spoof.SpoofViewModel
import kotlinx.coroutines.launch

@Composable
fun SpoofScreen(
    onNavigateUp: () -> Unit,
    onNavigateToSplash: () -> Unit,
    viewModel: SpoofViewModel = hiltViewModel()
) {
    ScreenContent(
        onNavigateUp = onNavigateUp,
        onNavigateToSplash = onNavigateToSplash,
        onDeviceSpoofImport = { uri -> viewModel.importDeviceSpoof(uri) },
        onDeviceSpoofExport = { uri -> viewModel.exportDeviceSpoof(uri) }
    )
}

@Composable
private fun ScreenContent(
    pages: List<SpoofPage> = listOf(SpoofPage.DEVICE, SpoofPage.LOCALE),
    onNavigateUp: () -> Unit = {},
    onNavigateToSplash: () -> Unit = {},
    onDeviceSpoofImport: (uri: Uri) -> Unit = {},
    onDeviceSpoofExport: (uri: Uri) -> Unit = {}
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState { pages.size }
    val coroutineScope = rememberCoroutineScope()
    val snackBarHostState = remember { SnackbarHostState() }

    val docImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = {
            if (it != null) {
                onDeviceSpoofImport(it)
            } else {
                context.toast(R.string.toast_import_failed)
            }
        }
    )
    val docExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(Constants.PROPERTIES_EXPORT_MIME_TYPE),
        onResult = {
            if (it != null) {
                onDeviceSpoofExport(it)
            } else {
                context.toast(R.string.toast_export_failed)
            }
        }
    )

    fun onRequestNavigateToSplash() {
        coroutineScope.launch {
            val result = snackBarHostState.showSnackbar(
                message = context.getString(R.string.force_restart_snack),
                actionLabel = context.getString(R.string.action_restart),
                duration = SnackbarDuration.Indefinite
            )
            when (result) {
                SnackbarResult.ActionPerformed -> {
                    AccountProvider.logout(context)
                    onNavigateToSplash()
                }

                else -> Unit
            }
        }
    }

    @Composable
    fun SetupMenu() {
        SpoofMenu { menuItem ->
            when (menuItem) {
                MenuItem.IMPORT -> {
                    docImportLauncher.launch(arrayOf(Constants.PROPERTIES_IMPORT_MIME_TYPE))
                }

                MenuItem.EXPORT -> {
                    docExportLauncher.launch(
                        "aurora_store_${Build.BRAND}_${Build.DEVICE}.properties"
                    )
                }
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackBarHostState)
        },
        topBar = {
            TopAppBar(
                title = stringResource(R.string.title_spoof_manager),
                onNavigateUp = onNavigateUp,
                actions = { SetupMenu() }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SecondaryTabRow(
                modifier = Modifier.fillMaxWidth(),
                selectedTabIndex = pagerState.currentPage
            ) {
                pages.fastForEachIndexed { index, _ ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = {
                            Text(text = stringResource(id = pages[index].localized))
                        }
                    )
                }
            }
            HorizontalPager(
                modifier = Modifier.fillMaxSize(),
                state = pagerState,
                verticalAlignment = Alignment.Top
            ) { page ->
                when (pages[page]) {
                    SpoofPage.DEVICE -> DevicePage(
                        onRequestNavigateToSplash = ::onRequestNavigateToSplash
                    )

                    SpoofPage.LOCALE -> LocalePage(
                        onRequestNavigateToSplash = ::onRequestNavigateToSplash
                    )
                }
            }
        }
    }
}
