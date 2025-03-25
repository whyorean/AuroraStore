/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurora.gplayapi.data.models.Review
import com.aurora.store.compose.composables.TopAppBarComposable
import com.aurora.store.viewmodel.details.AppDetailsViewModel

@Composable
fun DetailsReviewScreen(
    onNavigateUp: () -> Unit,
    viewModel: AppDetailsViewModel = hiltViewModel()
) {
    val app by viewModel.app.collectAsStateWithLifecycle(null)

    ScreenContent(
        topAppBarTitle = app!!.displayName,
        onNavigateUp = onNavigateUp
    )
}

@Composable
private fun ScreenContent(
    topAppBarTitle: String? = null,
    onNavigateUp: () -> Unit = {},
    reviews: List<Review> = emptyList()
) {
    Scaffold(
        topBar = {
            TopAppBarComposable(title = topAppBarTitle, onNavigateUp = onNavigateUp)
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {

        }
    }
}

@Preview
@Composable
private fun DetailsReviewScreenPreview() {
    ScreenContent()
}
