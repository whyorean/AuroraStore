/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.preferences.installation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurora.store.R
import com.aurora.store.compose.composable.InstallerListItem
import com.aurora.store.compose.composable.TopAppBar
import com.aurora.store.compose.preview.PreviewTemplate
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.data.installer.SessionInstaller
import com.aurora.store.data.model.Installer
import com.aurora.store.data.model.InstallerInfo
import com.aurora.store.viewmodel.preferences.InstallerViewModel

@Composable
fun InstallerScreen(onNavigateUp: () -> Unit, viewModel: InstallerViewModel = hiltViewModel()) {
    val currentInstallerId by viewModel.currentInstaller.collectAsStateWithLifecycle()
    val snackBarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = Unit) {
        viewModel.error.collect { error ->
            snackBarHostState.showSnackbar(message = error)
        }
    }

    ScreenContent(
        onNavigateUp = onNavigateUp,
        snackBarHostState = snackBarHostState,
        currentInstaller = Installer.entries[currentInstallerId],
        availableInstallers = AppInstaller.getAvailableInstallersInfo(LocalContext.current),
        onInstallerSelected = { installer -> viewModel.save(installer) }
    )
}

@Composable
private fun ScreenContent(
    onNavigateUp: () -> Unit = {},
    snackBarHostState: SnackbarHostState = SnackbarHostState(),
    currentInstaller: Installer = Installer.SESSION,
    availableInstallers: List<InstallerInfo> = emptyList(),
    onInstallerSelected: (installer: Installer) -> Unit = {}
) {
    val snackBarHostState = remember { snackBarHostState }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackBarHostState)
        },
        topBar = {
            TopAppBar(
                title = stringResource(R.string.pref_install_mode_title),
                onNavigateUp = onNavigateUp
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(vertical = dimensionResource(R.dimen.padding_medium))
        ) {
            items(items = availableInstallers, key = { i -> i.id }) { installerInfo ->
                InstallerListItem(
                    installerInfo = installerInfo,
                    isSelected = installerInfo.installer == currentInstaller,
                    onClick = { onInstallerSelected(installerInfo.installer) }
                )
            }
        }
    }
}

@Preview
@Composable
private fun InstallerScreenPreview() {
    PreviewTemplate {
        ScreenContent(
            availableInstallers = listOf(SessionInstaller.installerInfo)
        )
    }
}
