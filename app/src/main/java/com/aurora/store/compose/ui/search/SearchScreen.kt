/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.search

import android.os.Build
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.rememberContainedSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.aurora.extensions.requiresGMS
import com.aurora.gplayapi.SearchSuggestEntry
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.compose.composable.ContainedLoadingIndicator
import com.aurora.store.compose.composable.ExpressiveMenuGroup
import com.aurora.store.compose.composable.ExpressiveMenuIcon
import com.aurora.store.compose.composable.ExpressiveMenuPopup
import com.aurora.store.compose.composable.ExpressiveSelectableMenuItem
import com.aurora.store.compose.composable.Placeholder
import com.aurora.store.compose.composable.auroraListItemShapes
import com.aurora.store.compose.composable.ScrollHint
import com.aurora.store.compose.composable.SearchSuggestionListItem
import com.aurora.store.compose.navigation.Destination
import com.aurora.store.compose.ui.details.AppDetailsScreen
import com.aurora.store.data.model.SearchFilter
import com.aurora.store.viewmodel.search.SearchViewModel
import com.aurora.store.util.CommonUtil
import coil3.compose.AsyncImage
import com.aurora.store.compose.composable.rememberStoreImageRequest
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The search action uses the same expressive floating-toolbar FAB slot as the bottom bar. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainSearchButton(
    onNavigateTo: (Destination) -> Unit
) {
    val colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors()
    FloatingActionButton(
        onClick = { onNavigateTo(Destination.Search()) },
        modifier = Modifier.size(FloatingToolbarDefaults.ContainerSize),
        shape = FloatingActionButtonDefaults.shape,
        containerColor = colors.fabContainerColor,
        contentColor = colors.fabContentColor,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 8.dp,
            pressedElevation = 10.dp,
            focusedElevation = 8.dp,
            hoveredElevation = 10.dp
        )
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_round_search),
            contentDescription = stringResource(R.string.action_search),
            modifier = Modifier.size(28.dp)
        )
    }
}

/**
 * Opens the search destination before the IME gets Back. That keeps a single Back press
 * consistent even while the search keyboard is visible.
 */
@Composable
private fun SearchBackHandler(
    enabled: Boolean,
    onBack: () -> Unit
) {
    val view = LocalView.current
    val currentOnBack by rememberUpdatedState(onBack)
    val usePlatformPriority = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    DisposableEffect(enabled, view, usePlatformPriority) {
        val dispatcher = if (enabled && usePlatformPriority) {
            view.findOnBackInvokedDispatcher()
        } else {
            null
        }
        val callback = OnBackInvokedCallback { currentOnBack() }
        dispatcher?.registerOnBackInvokedCallback(
            OnBackInvokedDispatcher.PRIORITY_OVERLAY,
            callback
        )
        onDispose { dispatcher?.unregisterOnBackInvokedCallback(callback) }
    }

    BackHandler(enabled = enabled && !usePlatformPriority, onBack = onBack)
}

@Composable
fun SearchScreen(
    initialQuery: String? = null,
    onNavigateBack: () -> Unit = {},
    onNavigateTo: (Destination) -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel()
) {
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val normalizedInitialQuery = remember(initialQuery) {
        initialQuery?.trim()?.takeIf(String::isNotEmpty)
    }
    val textFieldState = rememberTextFieldState(initialText = normalizedInitialQuery.orEmpty())
    var submittedQuery by rememberSaveable(normalizedInitialQuery) {
        mutableStateOf(normalizedInitialQuery.orEmpty())
    }
    val searchBarState = rememberContainedSearchBarState(
        initialValue = SearchBarValue.Expanded
    )
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<String>()
    val coroutineScope = rememberCoroutineScope()
    var backHandled by remember { mutableStateOf(false) }

    LaunchedEffect(normalizedInitialQuery) {
        if (normalizedInitialQuery != null) {
            viewModel.search(normalizedInitialQuery)
        } else {
            viewModel.fetchSuggestions("")
        }
    }

    LaunchedEffect(textFieldState, submittedQuery) {
        snapshotFlow { textFieldState.text.toString().trim() }
            .distinctUntilChanged()
            .collectLatest { query ->
                if (query == submittedQuery) return@collectLatest
                submittedQuery = ""
                if (query.isBlank()) {
                    viewModel.fetchSuggestions(query)
                } else {
                    delay(SUGGESTION_DEBOUNCE_MILLIS)
                    viewModel.fetchSuggestions(query)
                }
            }
    }

    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    fun submitSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        if (textFieldState.text.toString() != trimmed) {
            textFieldState.setTextAndPlaceCursorAtEnd(trimmed)
        }
        submittedQuery = trimmed
        viewModel.search(trimmed)
        keyboardController?.hide()
    }

    fun showDetailPane(packageName: String) {
        coroutineScope.launch {
            scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.Detail, packageName)
        }
    }

    fun handleBack() {
        if (scaffoldNavigator.currentDestination?.contentKey != null) {
            coroutineScope.launch { scaffoldNavigator.navigateBack() }
        } else if (!backHandled) {
            backHandled = true
            // Start dismissing the IME together with the panel exit. Waiting until the focused
            // field is disposed leaves the keyboard covering the Apps screen after Back.
            onNavigateBack()
            // The state change above starts the overlay exit immediately; IME dismissal can
            // proceed alongside it without delaying the first animated frame.
            keyboardController?.hide()
        }
    }

    SearchBackHandler(enabled = true, onBack = ::handleBack)

    NavigableListDetailPaneScaffold(
        navigator = scaffoldNavigator,
        listPane = { AnimatedPane {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.surface,
                contentWindowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                )
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = dimensionResource(R.dimen.spacing_large),
                                vertical = dimensionResource(R.dimen.spacing_small)
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = SearchBarDefaults.inputFieldShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            SearchBarDefaults.InputField(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                                searchBarState = searchBarState,
                                textFieldState = textFieldState,
                                onSearch = ::submitSearch,
                                placeholder = {
                                    Text(
                                        text = stringResource(R.string.search_hint),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                leadingIcon = {
                                    IconButton(onClick = ::handleBack) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_arrow_back),
                                            contentDescription = stringResource(R.string.action_back)
                                        )
                                    }
                                },
                                trailingIcon = {
                                    if (textFieldState.text.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                submittedQuery = ""
                                                viewModel.fetchSuggestions("")
                                                textFieldState.clearText()
                                            }
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_cancel),
                                                contentDescription = stringResource(R.string.action_clear)
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }

                    if (submittedQuery.isNotBlank()) {
                        MainSearchResults(
                            viewModel = viewModel,
                            onOpenApp = ::showDetailPane
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().imePadding(),
                            contentPadding = PaddingValues(
                                top = dimensionResource(R.dimen.spacing_small),
                                bottom = WindowInsets.navigationBars.asPaddingValues()
                                    .calculateBottomPadding() + dimensionResource(R.dimen.spacing_small)
                            )
                        ) {
                            items(suggestions) { suggestion ->
                                SearchSuggestionListItem(
                                    searchSuggestEntry = suggestion,
                                    onClick = ::submitSearch,
                                    onAction = { query ->
                                        textFieldState.setTextAndPlaceCursorAtEnd(query.trim())
                                    }
                                )
                            }
                        }
                    }
                }
            }
        } },
        detailPane = { AnimatedPane {
            when (val packageName = scaffoldNavigator.currentDestination?.contentKey) {
                null -> Placeholder(
                    painter = painterResource(R.drawable.ic_round_search),
                    message = stringResource(R.string.select_app_for_details)
                )
                else -> AppDetailsScreen(
                    packageName = packageName,
                    onNavigateTo = { destination ->
                        if (destination is Destination.AppDetails) {
                            showDetailPane(destination.packageName)
                        } else {
                            onNavigateTo(destination)
                        }
                    },
                    forceSinglePane = true
                )
            }
        } }
    )
}

@Composable
private fun MainSearchResults(
    viewModel: SearchViewModel,
    onOpenApp: (String) -> Unit
) {
    val results = viewModel.apps.collectAsLazyPagingItems()

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        FilterHeader(
            isEnabled = results.loadState.refresh is LoadState.NotLoading,
            isAnonymous = viewModel.authProvider.isAnonymous,
            onFilter = viewModel::filterResults
        )

        when (val refreshState = results.loadState.refresh) {
            is LoadState.Loading -> {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    ContainedLoadingIndicator()
                }
            }

            is LoadState.Error -> {
                Placeholder(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    painter = painterResource(R.drawable.ic_refresh),
                    message = stringResource(R.string.error),
                    actionLabel = stringResource(R.string.action_retry),
                    onAction = { results.retry() }
                )
            }

            else -> {
                if (results.itemCount == 0) {
                    Placeholder(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        painter = painterResource(R.drawable.ic_disclaimer),
                        message = stringResource(R.string.no_apps_available)
                    )
                } else {
                    val listState = rememberLazyListState()
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                top = 4.dp,
                                bottom = WindowInsets.navigationBars.asPaddingValues()
                                    .calculateBottomPadding() + 4.dp
                            )
                        ) {
                            items(
                                count = results.itemCount,
                                key = { index ->
                                    results.peek(index)?.packageName ?: "main-search-result-$index"
                                }
                            ) { index ->
                                results[index]?.let { app ->
                                    SearchResultListItem(
                                        app = app,
                                        onClick = { onOpenApp(app.packageName) }
                                    )
                                    if (index < results.itemCount - 1) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(
                                                start = dimensionResource(R.dimen.spacing_large) +
                                                    dimensionResource(R.dimen.icon_size_medium) +
                                                    dimensionResource(R.dimen.spacing_large)
                                            ),
                                            color = MaterialTheme.colorScheme.outlineVariant
                                        )
                                    }
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

private const val SUGGESTION_DEBOUNCE_MILLIS = 180L

@Composable
private fun SearchResultListItem(
    modifier: Modifier = Modifier,
    app: App,
    onClick: () -> Unit = {}
) {
    ListItem(
        onClick = onClick,
        modifier = modifier,
        shapes = auroraListItemShapes(),
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = app.developerName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildSearchAppExtras(app),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        leadingContent = {
            AsyncImage(
                modifier = Modifier
                    .requiredSize(dimensionResource(R.dimen.icon_size_medium))
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.app_icon_radius))),
                model = rememberStoreImageRequest(app.iconArtwork.url),
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
        },
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(
            horizontal = dimensionResource(R.dimen.spacing_large),
            vertical = dimensionResource(R.dimen.spacing_small)
        ),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    ) {
        Text(
            text = app.displayName,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun buildSearchAppExtras(app: App): String {
    val priceLabel = stringResource(if (app.isFree) R.string.details_free else R.string.details_paid)
    val adsLabel = stringResource(
        if (app.containsAds) R.string.details_contains_ads else R.string.details_no_ads
    )
    val gmsLabel = stringResource(R.string.details_gsf_dependent)
    val requiresGms = app.requiresGMS()

    return remember(
        app.size,
        app.downloadString,
        app.labeledRating,
        app.isFree,
        app.containsAds,
        requiresGms,
        priceLabel,
        adsLabel,
        gmsLabel
    ) {
        buildList {
            add(if (app.size > 0) CommonUtil.addSiPrefix(app.size) else app.downloadString)
            add("${app.labeledRating}★")
            add(priceLabel)
            add(adsLabel)
            if (requiresGms) add(gmsLabel)
        }.joinToString(separator = "  •  ")
    }
}

@Composable
private fun FilterHeader(
    isAnonymous: Boolean = true,
    isEnabled: Boolean = true,
    onFilter: (filter: SearchFilter) -> Unit
) {
    var activeFilter by rememberSaveable { mutableStateOf(SearchFilter()) }

    val filters = remember(isAnonymous) {
        listOfNotNull(
            R.string.action_filter_rating,
            R.string.app_info_downloads,
            R.string.details_free,
            R.string.action_filter_no_ads,
            if (!isAnonymous) R.string.action_filter_no_gms else null
        )
    }

    val downloadLabels = stringArrayResource(R.array.filterDownloadsLabels)
    val downloadValues = stringArrayResource(R.array.filterDownloadsValues)
    val ratingLabels = stringArrayResource(R.array.filterRatingLabels)
    val ratingValues = stringArrayResource(R.array.filterRatingValues)

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
                        contentDescription = null
                    )
                }
            }
        )
    }

    @Composable
    fun ExpandableFilterChip(@StringRes filter: Int, isSelected: Boolean) {
        var isExpanded by rememberSaveable { mutableStateOf(false) }

        val labels = when (filter) {
            R.string.action_filter_rating -> ratingLabels
            else -> downloadLabels
        }
        val values = when (filter) {
            R.string.action_filter_rating -> ratingValues
            else -> downloadValues
        }
        val options = remember(filter, ratingLabels, ratingValues, downloadLabels, downloadValues) {
            labels.zip(values).associate { (label, value) -> label.trim() to value }
        }
        val selectedValue = when (filter) {
            R.string.action_filter_rating -> activeFilter.minRating.toString()
            else -> activeFilter.minInstalls.toString()
        }
        val selectedLabel = if (isSelected) {
            options.entries.firstOrNull { it.value == selectedValue }?.key?.trim()
        } else {
            null
        }
        val filterLabel = stringResource(filter)

        Box {
            FilterChip(
                enabled = isEnabled,
                onClick = { isExpanded = !isExpanded },
                label = {
                    Text(
                        text = buildString {
                            append(filterLabel)
                            if (selectedLabel != null) {
                                append(" · ")
                                append(selectedLabel)
                            }
                        },
                        maxLines = 1
                    )
                },
                selected = isSelected,
                leadingIcon = {
                    if (isSelected) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = null
                        )
                    }
                },
                trailingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_drop_down),
                        contentDescription = null
                    )
                }
            )

            ExpressiveMenuPopup(
                expanded = isExpanded,
                onDismissRequest = { isExpanded = false }
            ) {
                ExpressiveMenuGroup(index = 0, count = 1) {
                    options.entries.forEachIndexed { index, (key, value) ->
                        val selected = when (filter) {
                            R.string.action_filter_rating -> {
                                value.toFloatOrNull() == activeFilter.minRating
                            }

                            else -> value.toLongOrNull() == activeFilter.minInstalls
                        }
                        ExpressiveSelectableMenuItem(
                            index = index,
                            count = options.size,
                            selected = selected,
                            text = { Text(text = key) },
                            selectedLeadingIcon = {
                                ExpressiveMenuIcon(painterResource(R.drawable.ic_check))
                            },
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
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.spacing_large)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
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
