/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurora.gplayapi.data.models.App
import com.aurora.store.compose.composables.TopAppBarComposable
import com.aurora.store.viewmodel.details.AppDetailsViewModel

@Composable
fun AppDetailsScreen(
    packageName: String,
    onNavigateUp: () -> Unit,
    viewModel: AppDetailsViewModel = hiltViewModel()
) {
    val app by viewModel.app.collectAsStateWithLifecycle(null)

    LaunchedEffect(key1 = Unit) { viewModel.fetchAppDetails(packageName) }

    ScreenContent(
        app = app,
        onNavigateUp = onNavigateUp
    )
}

@Composable
private fun ScreenContent(
    app: App? = null,
    onNavigateUp: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBarComposable(
                onNavigateUp = onNavigateUp
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            if (app == null) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    CircularProgressIndicator()
                }
                return@Column
            }

        }
    }
}

@Preview
@Composable
private fun AppDetailsScreenPreview() {
    ScreenContent()
}
