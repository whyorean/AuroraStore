/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.LocalAsyncImagePreviewHandler
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.compose.composables.HeaderComposable
import com.aurora.store.compose.composables.TopAppBarComposable
import com.aurora.store.compose.composables.app.AppListComposable
import com.aurora.store.compose.composables.preview.AppPreviewProvider
import com.aurora.store.compose.composables.preview.coilPreviewProvider
import com.aurora.store.viewmodel.details.AppDetailsViewModel
import kotlin.random.Random

/**
 * Screen to display similar and related suggestions for an app, supposed to be used internally
 * by the [AppDetailsScreen] and not navigable from other screens
 */
@Composable
fun DetailsSuggestions(
    onNavigateUp: (() -> Unit)?,
    viewModel: AppDetailsViewModel = hiltViewModel(),
    onNavigateToAppDetails: (packageName: String) -> Unit
) {
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()

    ScreenContent(
        onNavigateUp = onNavigateUp,
        suggestions = suggestions,
        onNavigateToAppDetails = onNavigateToAppDetails
    )
}

@Composable
private fun ScreenContent(
    onNavigateUp: (() -> Unit)? = {},
    suggestions: List<App> = emptyList(),
    onNavigateToAppDetails: (packageName: String) -> Unit = {}
) {
    Scaffold(
        topBar = { TopAppBarComposable(onNavigateUp = onNavigateUp) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier.padding(dimensionResource(R.dimen.margin_medium)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_suggestions),
                    contentDescription = null
                )
                HeaderComposable(title = stringResource(R.string.pref_ui_similar_apps))
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = dimensionResource(R.dimen.padding_medium))
            ) {
                items(items = suggestions, key = { item -> item.id }) { app ->
                    AppListComposable(
                        app = app,
                        onClick = { onNavigateToAppDetails(app.packageName) }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
@OptIn(ExperimentalCoilApi::class)
private fun DetailsSuggestionsPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    CompositionLocalProvider(LocalAsyncImagePreviewHandler provides coilPreviewProvider) {
        ScreenContent(
            onNavigateUp = null,
            suggestions = List(10) { app.copy(id = Random.nextInt()) }
        )
    }
}
