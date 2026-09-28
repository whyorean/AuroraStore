/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.apps

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aurora.gplayapi.helpers.contracts.StreamContract
import com.aurora.gplayapi.helpers.contracts.TopChartsContract
import com.aurora.store.R
import com.aurora.store.compose.navigation.Destination
import com.aurora.store.util.Preferences
import com.aurora.store.viewmodel.category.CategoryViewModel
import com.aurora.store.viewmodel.homestream.StreamViewModel
import com.aurora.store.viewmodel.topchart.TopChartViewModel
import kotlinx.coroutines.launch

internal fun category(pageType: Int): StreamContract.Category =
    if (pageType == 1) StreamContract.Category.GAME else StreamContract.Category.APPLICATION

private enum class AppsTab(
    @StringRes val titleRes: Int,
    val iconRes: Int
) {
    FOR_YOU(R.string.tab_for_you, R.drawable.ic_apps),
    TOP_CHARTS(R.string.tab_top_charts, R.drawable.ic_star),
    CATEGORIES(R.string.tab_categories, R.drawable.ic_list_check)
}

private val chartTitles = listOf(
    R.string.tab_top_free,
    R.string.tab_top_grossing,
    R.string.tab_trending,
    R.string.tab_top_paid
)

private val chartIcons = listOf(
    R.drawable.ic_apps,
    R.drawable.ic_paid,
    R.drawable.ic_updates,
    R.drawable.ic_paid
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExpressiveChoiceGroup(
    labels: List<Int>,
    icons: List<Int>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    maxItemsPerRow: Int = labels.size,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        labels.indices.chunked(maxItemsPerRow.coerceAtLeast(1)).forEach { rowIndices ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowIndices.forEach { index ->
                    val selected = selectedIndex == index
                    FilterChip(
                        selected = selected,
                        onClick = { onSelect(index) },
                        label = { Text(stringResource(labels[index]), maxLines = 1) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(icons[index]),
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        },
                        shapes = FilterChipDefaults.shapes(),
                        colors = FilterChipDefaults.tonalFilterChipColors(),
                        elevation = null
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppsGamesScreen(
    pageType: Int,
    streamViewModel: StreamViewModel = hiltViewModel(key = "stream_$pageType"),
    topChartViewModel: TopChartViewModel = hiltViewModel(key = "topChart_$pageType"),
    categoryViewModel: CategoryViewModel = hiltViewModel(key = "category_$pageType"),
    onNavigateTo: (Destination) -> Unit = {}
) {
    val context = LocalContext.current

    val isForYouEnabled = Preferences.getBoolean(context, Preferences.PREFERENCE_FOR_YOU)
    val tabs = remember(isForYouEnabled) {
        buildList {
            if (isForYouEnabled) add(AppsTab.FOR_YOU)
            add(AppsTab.TOP_CHARTS)
            add(AppsTab.CATEGORIES)
        }
    }
    val tabLabels = remember(tabs) { tabs.map(AppsTab::titleRes) }
    val tabIcons = remember(tabs) { tabs.map(AppsTab::iconRes) }
    val pagerState = rememberPagerState { tabs.size }
    val coroutineScope = rememberCoroutineScope()
    var selectedChartIndex by rememberSaveable { mutableIntStateOf(0) }

    val chartType = if (pageType == 1) {
        TopChartsContract.Type.GAME
    } else {
        TopChartsContract.Type.APPLICATION
    }
    LaunchedEffect(Unit) {
        topChartViewModel.getStreamCluster(
            chartType,
            TopChartsContract.Chart.TOP_SELLING_FREE
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ExpressiveChoiceGroup(
            labels = tabLabels,
            icons = tabIcons,
            selectedIndex = pagerState.targetPage,
            onSelect = { index ->
                coroutineScope.launch {
                    pagerState.animateScrollToPage(
                        page = index,
                        animationSpec = tween(
                            durationMillis = 280,
                            easing = LinearOutSlowInEasing
                        )
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        AnimatedVisibility(
            visible = tabs.getOrNull(pagerState.targetPage) == AppsTab.TOP_CHARTS,
            enter = expandVertically(
                animationSpec = tween(durationMillis = 160, easing = LinearOutSlowInEasing)
            ) + fadeIn(tween(durationMillis = 160)),
            exit = shrinkVertically(
                animationSpec = tween(durationMillis = 120, easing = LinearOutSlowInEasing)
            ) + fadeOut(tween(durationMillis = 120))
        ) {
            ExpressiveChoiceGroup(
                labels = chartTitles,
                icons = chartIcons,
                selectedIndex = selectedChartIndex,
                onSelect = { selectedChartIndex = it },
                maxItemsPerRow = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )
        }

        HorizontalPager(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            state = pagerState,
            verticalAlignment = Alignment.Top,
            userScrollEnabled = false
        ) { page ->
            when (tabs[page]) {
                AppsTab.FOR_YOU -> ForYouContent(
                    pageType = pageType,
                    viewModel = streamViewModel,
                    onAppClick = { onNavigateTo(Destination.AppDetails(it.packageName)) },
                    onHeaderClick = { onNavigateTo(Destination.StreamBrowse(it)) },
                    onClusterScrolled = { cluster ->
                        streamViewModel.observeCluster(category(pageType), cluster)
                    },
                    onScrolledToEnd = {
                        streamViewModel.observe(category(pageType), StreamContract.Type.HOME)
                    },
                    bottomContentPadding = 148.dp
                )

                AppsTab.TOP_CHARTS -> TopChartsContent(
                    pageType = pageType,
                    viewModel = topChartViewModel,
                    onAppClick = { onNavigateTo(Destination.AppDetails(it.packageName)) },
                    selectedIndex = selectedChartIndex,
                    bottomContentPadding = 148.dp
                )

                AppsTab.CATEGORIES -> CategoriesContent(
                    pageType = pageType,
                    viewModel = categoryViewModel,
                    onCategoryClick = { onNavigateTo(Destination.CategoryBrowse(it)) },
                    bottomContentPadding = 148.dp
                )
            }
        }
    }
}
