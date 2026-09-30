/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.search

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExpandedDockedSearchBar
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.aurora.extensions.emptyPagingItems
import com.aurora.gplayapi.SearchSuggestEntry
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.compose.composable.ContainedLoadingIndicator
import com.aurora.store.compose.composable.Placeholder
import com.aurora.store.compose.composable.ScrollHint
import com.aurora.store.compose.composable.SearchSuggestionListItem
import com.aurora.store.compose.composable.app.LargeAppListItem
import com.aurora.store.compose.composition.LocalUI
import com.aurora.store.compose.composition.UI
import com.aurora.store.compose.navigation.Destination
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.ThemePreviewProvider
import com.aurora.store.compose.ui.details.AppDetailsScreen
import com.aurora.store.data.model.SearchFilter
import com.aurora.store.viewmodel.search.SearchViewModel
import kotlin.random.Random
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(viewModel: SearchViewModel = hiltViewModel()) {
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val results = viewModel.apps.collectAsLazyPagingItems()

    val onSearchCallback = remember<(String) -> Unit> { { query -> viewModel.search(query) } }
    val onFetchSuggestionsCallback =
        remember<(String) -> Unit> { { query -> viewModel.fetchSuggestions(query) } }

    ScreenContent(
        suggestions = suggestions,
        results = results,
        onSearch = onSearchCallback,
        onFetchSuggestions = onFetchSuggestionsCallback,
        onFilter = { filter -> viewModel.filterResults(filter) },
        isAnonymous = viewModel.authProvider.isAnonymous
    )
}

@Composable
private fun ScreenContent(
    suggestions: List<SearchSuggestEntry> = emptyList(),
    results: LazyPagingItems<App> = emptyPagingItems(),
    onFetchSuggestions: (String) -> Unit = {},
    onSearch: (String) -> Unit = {},
    onFilter: (filter: SearchFilter) -> Unit = {},
    isAnonymous: Boolean = true
) {
    val activity = LocalActivity.current as? ComponentActivity
    val focusManager = LocalFocusManager.current
    val isTv = LocalUI.current == UI.TV
    val textFieldState = rememberTextFieldState()
    val searchBarState = rememberSearchBarState(initialValue = SearchBarValue.Expanded)
    var isSearching by rememberSaveable { mutableStateOf(false) }

    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<String>()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(key1 = textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .collectLatest { query -> onFetchSuggestions(query) }
    }

    fun showDetailPane(packageName: String) {
        coroutineScope.launch {
            scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.Detail, packageName)
        }
    }

    fun onRequestSearch(query: String) {
        val trimmed = query.trim()
        if (textFieldState.text.toString() != trimmed) {
            textFieldState.setTextAndPlaceCursorAtEnd(trimmed)
        }
        onSearch(trimmed)
        isSearching = true
        coroutineScope.launch { searchBarState.animateToCollapsed() }
    }

    @Composable
    fun SearchBar() {
        val inputField = @Composable {
            SearchBarDefaults.InputField(
                // On TV the collapsed field is reachable with the D-pad (see below), so the
                // keyboard must not come along: it would sit on top of the results. The center
                // key opens the docked bar instead, which is expanded and thus allowed to ask
                // for the keyboard.
                keyboardOptions = KeyboardOptions(
                    showKeyboardOnFocus = !isTv ||
                        searchBarState.targetValue == SearchBarValue.Expanded
                ),
                // Only allow focus while expanded. Otherwise the collapsed field
                // grabs focus whenever it is restored (returning from details,
                // dismissing the popup on Android 8, ...) and the search bar
                // reopens on its own. Tapping still expands via click detection.
                // TV is the exception: a remote reaches the field with the D-pad, and M3 only
                // expands on focus in touch mode, so focus alone leaves the bar collapsed.
                modifier = Modifier
                    .focusProperties {
                        canFocus = isTv ||
                            searchBarState.targetValue == SearchBarValue.Expanded
                    }
                    .onPreviewKeyEvent { event ->
                        if (!isTv ||
                            event.type != KeyEventType.KeyDown ||
                            searchBarState.currentValue != SearchBarValue.Collapsed
                        ) {
                            return@onPreviewKeyEvent false
                        }
                        when (event.key) {
                            // Down on a collapsed bar means "results". M3 would expand it
                            // again and take the focus (and the keyboard) back to the field.
                            Key.DirectionDown, Key.NumPadDirectionDown -> {
                                focusManager.moveFocus(FocusDirection.Down)
                                true
                            }

                            // The docked copy takes the focus and, being expanded, is the one
                            // allowed to ask for the keyboard.
                            Key.DirectionCenter -> {
                                coroutineScope.launch { searchBarState.animateToExpanded() }
                                true
                            }

                            else -> false
                        }
                    },
                searchBarState = searchBarState,
                textFieldState = textFieldState,
                onSearch = { query -> onRequestSearch(query) },
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_hint),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    IconButton(onClick = { activity?.onBackPressedDispatcher?.onBackPressed() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                trailingIcon = {
                    if (textFieldState.text.isNotBlank()) {
                        IconButton(onClick = { textFieldState.clearText() }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_cancel),
                                contentDescription = stringResource(R.string.action_clear)
                            )
                        }
                    }
                }
            )
        }

        AppBarWithSearch(
            state = searchBarState,
            inputField = inputField,
            colors = SearchBarDefaults.appBarWithSearchColors(
                appBarContainerColor = Color.Transparent
            )
        )
        ExpandedDockedSearchBar(state = searchBarState, inputField = inputField) {
            suggestions.forEach { suggestion ->
                SearchSuggestionListItem(
                    searchSuggestEntry = suggestion,
                    onClick = { query -> onRequestSearch(query) },
                    onAction = { query -> textFieldState.setTextAndPlaceCursorAtEnd(query.trim()) }
                )
            }
        }
    }

    @Composable
    fun ListPane() {
        Scaffold(
            topBar = { SearchBar() }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(vertical = dimensionResource(R.dimen.spacing_medium))
            ) {
                FilterHeader(
                    isEnabled = isSearching && results.loadState.refresh is LoadState.NotLoading,
                    isAnonymous = isAnonymous,
                    onFilter = onFilter
                )

                when (results.loadState.refresh) {
                    is LoadState.Loading -> ContainedLoadingIndicator()

                    is LoadState.Error -> {
                        Placeholder(
                            modifier = Modifier.padding(paddingValues),
                            painter = painterResource(R.drawable.ic_refresh),
                            message = stringResource(R.string.error),
                            actionLabel = stringResource(R.string.action_retry),
                            onAction = { results.retry() }
                        )
                    }

                    else -> {
                        if (isSearching && results.itemCount == 0) {
                            Placeholder(
                                modifier = Modifier.padding(paddingValues),
                                painter = painterResource(R.drawable.ic_disclaimer),
                                message = stringResource(R.string.no_apps_available)
                            )
                        } else {
                            val listState = rememberLazyListState()
                            Box(modifier = Modifier.fillMaxSize()) {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(
                                        dimensionResource(R.dimen.spacing_medium)
                                    )
                                ) {
                                    items(
                                        count = results.itemCount,
                                        key = { Uuid.random().toString() }
                                    ) { index ->
                                        results[index]?.let { app ->
                                            LargeAppListItem(
                                                app = app,
                                                onClick = { showDetailPane(app.packageName) }
                                            )
                                        }
                                    }
                                }
                                ScrollHint(
                                    listState = listState,
                                    modifier = Modifier.align(Alignment.BottomCenter)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun DetailPane() {
        with(scaffoldNavigator.currentDestination?.contentKey) {
            when {
                this != null -> {
                    AppDetailsScreen(
                        packageName = this,
                        onNavigateTo = { destination ->
                            if (destination is Destination.AppDetails) {
                                showDetailPane(destination.packageName)
                            }
                        },
                        forceSinglePane = true
                    )
                }

                else -> {
                    if (isSearching && results.itemCount > 0) {
                        Placeholder(
                            painter = painterResource(R.drawable.ic_round_search),
                            message = stringResource(R.string.select_app_for_details)
                        )
                    }
                }
            }
        }
    }

    NavigableListDetailPaneScaffold(
        navigator = scaffoldNavigator,
        listPane = { AnimatedPane { ListPane() } },
        detailPane = { AnimatedPane { DetailPane() } }
    )
}

@Composable
private fun FilterHeader(
    isAnonymous: Boolean = true,
    isEnabled: Boolean = true,
    onFilter: (filter: SearchFilter) -> Unit
) {
    var activeFilter by rememberSaveable { mutableStateOf(SearchFilter()) }

    val filters = listOfNotNull(
        R.string.action_filter_rating,
        R.string.app_info_downloads,
        R.string.details_free,
        R.string.action_filter_no_ads,
        if (!isAnonymous) R.string.action_filter_no_gms else null
    )

    @Composable
    fun NonExpandableFilterChip(@StringRes filter: Int, isSelected: Boolean) {
        FilterChip(
            enabled = isEnabled,
            onClick = {
                activeFilter = when (filter) {
                    R.string.details_free -> activeFilter.copy(isFree = !activeFilter.isFree)
                    R.string.action_filter_no_ads -> activeFilter.copy(noAds = !activeFilter.noAds)
                    else -> activeFilter.copy(noGMS = !activeFilter.noGMS)
                }
                onFilter(activeFilter)
            },
            label = { Text(text = stringResource(filter)) },
            selected = isSelected,
            leadingIcon = {
                if (isSelected) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = stringResource(filter)
                    )
                }
            }
        )
    }

    @Composable
    fun ExpandableFilterChip(@StringRes filter: Int, isSelected: Boolean) {
        var isExpanded by rememberSaveable { mutableStateOf(false) }

        Box {
            FilterChip(
                enabled = isEnabled,
                onClick = { isExpanded = !isExpanded },
                label = { Text(text = stringResource(filter)) },
                selected = isSelected,
                leadingIcon = {
                    if (isSelected) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = stringResource(filter)
                        )
                    }
                },
                trailingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_drop_down),
                        contentDescription = stringResource(filter)
                    )
                }
            )

            DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
                val downloadLabels = stringArrayResource(R.array.filterDownloadsLabels)
                val downloadValues = stringArrayResource(R.array.filterDownloadsValues)
                val ratingLabels = stringArrayResource(R.array.filterRatingLabels)
                val ratingValues = stringArrayResource(R.array.filterRatingValues)

                val options = when (filter) {
                    R.string.action_filter_rating -> ratingLabels.zip(ratingValues).toMap()
                    R.string.app_info_downloads -> downloadLabels.zip(downloadValues).toMap()
                    else -> emptyMap()
                }

                options.forEach { (key, value) ->
                    DropdownMenuItem(
                        text = { Text(text = key) },
                        onClick = {
                            activeFilter = when (filter) {
                                R.string.action_filter_rating -> {
                                    activeFilter.copy(minRating = value.toFloat())
                                }

                                else -> activeFilter.copy(minInstalls = value.toLong())
                            }
                            onFilter(activeFilter)
                            isExpanded = false
                        }
                    )
                }
            }
        }
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.spacing_medium)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
    ) {
        items(items = filters, key = { item -> item }) { filter ->
            val isSelected = when (filter) {
                R.string.action_filter_rating -> activeFilter.minRating > 0.0
                R.string.app_info_downloads -> activeFilter.minInstalls > 0
                R.string.details_free -> activeFilter.isFree
                R.string.action_filter_no_ads -> activeFilter.noAds
                R.string.action_filter_no_gms -> activeFilter.noGMS
                else -> false
            }

            when (filter) {
                R.string.details_free,
                R.string.action_filter_no_ads,
                R.string.action_filter_no_gms -> {
                    NonExpandableFilterChip(filter = filter, isSelected = isSelected)
                }

                R.string.action_filter_rating,
                R.string.app_info_downloads -> {
                    ExpandableFilterChip(filter = filter, isSelected = isSelected)
                }
            }
        }
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@PreviewScreenSizes
@Composable
private fun SearchScreenPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    val apps = List(10) { app.copy(id = Random.nextInt()) }
    val results = MutableStateFlow(PagingData.from(apps)).collectAsLazyPagingItems()
    ScreenContent(results = results)
}
