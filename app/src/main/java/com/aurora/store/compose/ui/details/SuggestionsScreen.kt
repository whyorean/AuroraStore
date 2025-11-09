/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.LocalAsyncImagePreviewHandler
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.compose.composable.Header
import com.aurora.store.compose.composable.TopAppBar
import com.aurora.store.compose.composable.app.LargeAppListItem
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.coilPreviewProvider
import com.aurora.store.viewmodel.details.AppDetailsViewModel
import kotlin.random.Random

@Composable
fun SuggestionsScreen(
    packageName: String,
    onNavigateToAppDetails: (packageName: String) -> Unit,
    actions: @Composable (RowScope.() -> Unit) = {},
    viewModel: AppDetailsViewModel = hiltViewModel(key = packageName),
) {
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()

    ScreenContent(
        suggestions = suggestions,
        onNavigateToAppDetails = onNavigateToAppDetails,
        actions = actions
    )
}

@Composable
private fun ScreenContent(
    suggestions: List<App> = emptyList(),
    onNavigateToAppDetails: (packageName: String) -> Unit = {},
    actions: @Composable (RowScope.() -> Unit) = {}
) {
    Scaffold(
        topBar = { TopAppBar(actions = actions) }
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
                Header(title = stringResource(R.string.pref_ui_similar_apps))
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = dimensionResource(R.dimen.padding_medium))
            ) {
                items(items = suggestions, key = { item -> item.id }) { app ->
                    LargeAppListItem(
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
private fun SuggestionsScreenPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    CompositionLocalProvider(LocalAsyncImagePreviewHandler provides coilPreviewProvider) {
        val apps = List(10) { app.copy(id = Random.nextInt()) }
        ScreenContent(suggestions = apps)
    }
}
