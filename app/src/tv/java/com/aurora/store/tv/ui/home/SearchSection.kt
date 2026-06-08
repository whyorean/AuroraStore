/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.tv.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.tv.material3.Button
import androidx.tv.material3.Text as TvText
import com.aurora.store.R
import com.aurora.store.tv.ui.commons.AppCard
import com.aurora.store.viewmodel.search.SearchViewModel
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SearchSection(
    onAppClick: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    // Saved so a round-trip to an app's details (which disposes this composition) keeps the query
    // and result/suggestion state on return. `searched` flips to true once a query is submitted.
    var query by rememberSaveable { mutableStateOf("") }
    var searched by rememberSaveable { mutableStateOf(false) }

    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val results = viewModel.apps.collectAsLazyPagingItems()

    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    // Auto-focus the field and raise the keyboard when Search is first opened from the nav drawer.
    // Skipped once a search has run so returning from a result's details lands on the grid, not the
    // keyboard.
    LaunchedEffect(Unit) {
        if (!searched) {
            awaitFrame()
            runCatching { focusRequester.requestFocus() }
            keyboard?.show()
        }
    }

    // Fetch suggestions as the query changes; collectLatest cancels stale in-flight lookups.
    LaunchedEffect(Unit) {
        snapshotFlow { query }.collectLatest { viewModel.fetchSuggestions(it) }
    }

    fun runSearch(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        query = trimmed
        viewModel.search(trimmed)
        searched = true
        keyboard?.hide()
    }

    Column(
        modifier = modifier.fillMaxSize().padding(dimensionResource(R.dimen.spacing_xlarge)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
    ) {
        // Search only on the keyboard's Search action, not per keystroke: SearchViewModel.search()
        // builds a fresh pager and launches an uncancelled coroutine on every call.
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                searched = false
            },
            placeholder = { Text(stringResource(R.string.search_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { runSearch(query) }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
        )

        // Suggestions appear while typing and disappear once a search is submitted.
        if (!searched && suggestions.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(
                    dimensionResource(R.dimen.spacing_small)
                )
            ) {
                items(items = suggestions) { suggestion ->
                    Button(onClick = { runSearch(suggestion.title) }) {
                        TvText(text = suggestion.title)
                    }
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(dimensionResource(R.dimen.tv_app_card_width)),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium)),
            // contentPadding gives focused cards room to scale at the grid edges without clipping.
            contentPadding = PaddingValues(
                top = dimensionResource(R.dimen.spacing_large),
                bottom = dimensionResource(R.dimen.spacing_medium)
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                count = results.itemCount,
                key = results.itemKey { it.packageName }
            ) { index ->
                results[index]?.let { app ->
                    AppCard(
                        app = app,
                        onClick = { onAppClick(app.packageName) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
