/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.helpers.contracts.TopChartsContract
import com.aurora.store.R
import com.aurora.store.compose.composable.Placeholder
import com.aurora.store.compose.composable.ShimmerAppRow
import com.aurora.store.compose.composable.app.LargeAppListItem
import com.aurora.store.compose.composition.collectForced
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.ThemePreviewProvider
import com.aurora.store.data.model.ViewState
import com.aurora.store.data.model.ViewState.Loading.getDataAs
import com.aurora.store.viewmodel.topchart.TopChartViewModel

private const val LOAD_MORE_THRESHOLD = 2

@Composable
internal fun TopChartsContent(
    pageType: Int,
    viewModel: TopChartViewModel,
    onAppClick: (App) -> Unit
) {
    val charts = listOf(
        TopChartsContract.Chart.TOP_SELLING_FREE,
        TopChartsContract.Chart.TOP_GROSSING,
        TopChartsContract.Chart.MOVERS_SHAKERS,
        TopChartsContract.Chart.TOP_SELLING_PAID
    )
    val chartTitles = listOf(
        R.string.tab_top_free,
        R.string.tab_top_grossing,
        R.string.tab_trending,
        R.string.tab_top_paid
    )
    val chartType =
        if (pageType == 1) TopChartsContract.Type.GAME else TopChartsContract.Type.APPLICATION
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val selectedChart = charts[selectedIndex]
    val state by viewModel.state.collectForced(ViewState.Loading)
    val cluster = state.getDataAs<StreamCluster?>()
    val listState = rememberLazyListState()

    LaunchedEffect(selectedIndex) {
        listState.scrollToItem(0)
        viewModel.getStreamCluster(chartType, selectedChart)
    }

    val reachedEnd by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = listState.layoutInfo.totalItemsCount
            last >= total - LOAD_MORE_THRESHOLD
        }
    }
    LaunchedEffect(reachedEnd) {
        if (reachedEnd && cluster?.hasNext() == true) {
            viewModel.nextCluster(chartType, selectedChart)
        }
    }

    TopChartsBody(
        selectedIndex = selectedIndex,
        chartTitles = chartTitles,
        state = state,
        cluster = cluster,
        listState = listState,
        onTabSelected = { selectedIndex = it },
        onRetry = { viewModel.getStreamCluster(chartType, selectedChart) },
        onAppClick = onAppClick
    )
}

@Composable
private fun TopChartsBody(
    selectedIndex: Int,
    chartTitles: List<Int>,
    state: ViewState,
    cluster: StreamCluster?,
    listState: LazyListState = rememberLazyListState(),
    onTabSelected: (Int) -> Unit = {},
    onRetry: () -> Unit = {},
    onAppClick: (App) -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        SecondaryScrollableTabRow(
            selectedTabIndex = selectedIndex,
            edgePadding = dimensionResource(R.dimen.spacing_small)
        ) {
            chartTitles.forEachIndexed { index, titleRes ->
                Tab(
                    selected = selectedIndex == index,
                    onClick = { onTabSelected(index) },
                    text = { Text(stringResource(titleRes)) }
                )
            }
        }

        when {
            state is ViewState.Error -> Placeholder(
                modifier = Modifier.weight(1f),
                painter = painterResource(R.drawable.ic_apps),
                message = stringResource(R.string.no_apps_available),
                actionLabel = stringResource(R.string.action_retry),
                onAction = onRetry
            )

            cluster != null && cluster.clusterAppList.isEmpty() -> Placeholder(
                modifier = Modifier.weight(1f),
                painter = painterResource(R.drawable.ic_apps),
                message = stringResource(R.string.no_apps_available)
            )

            cluster != null -> {
                val apps = cluster.clusterAppList
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    state = listState,
                    contentPadding = PaddingValues(
                        vertical = dimensionResource(R.dimen.spacing_small)
                    ),
                    verticalArrangement = Arrangement.spacedBy(
                        dimensionResource(R.dimen.spacing_xsmall)
                    )
                ) {
                    items(count = apps.size, key = { apps[it].id }) { index ->
                        LargeAppListItem(
                            app = apps[index],
                            onClick = { onAppClick(apps[index]) }
                        )
                    }
                    if (cluster.hasNext()) {
                        item(key = "progress") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(dimensionResource(R.dimen.spacing_large)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }

            else -> LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = dimensionResource(R.dimen.spacing_small)),
                verticalArrangement = Arrangement.spacedBy(
                    dimensionResource(R.dimen.spacing_xsmall)
                )
            ) {
                items(8) { ShimmerAppRow() }
            }
        }
    }
}

private val previewChartTitles = listOf(
    R.string.tab_top_free,
    R.string.tab_top_grossing,
    R.string.tab_trending,
    R.string.tab_top_paid
)

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun TopChartsBodyLoadingPreview() {
    TopChartsBody(
        selectedIndex = 0,
        chartTitles = previewChartTitles,
        state = ViewState.Loading,
        cluster = null
    )
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun TopChartsBodyLoadedPreview() {
    val app = AppPreviewProvider().values.first()
    val apps = List(5) { i -> app.copy(id = i + 1, packageName = "com.preview.app$i") }
    val cluster = StreamCluster(clusterAppList = apps)
    TopChartsBody(
        selectedIndex = 0,
        chartTitles = previewChartTitles,
        state = ViewState.Success(cluster),
        cluster = cluster
    )
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun TopChartsBodyEmptyPreview() {
    val cluster = StreamCluster(clusterAppList = emptyList())
    TopChartsBody(
        selectedIndex = 1,
        chartTitles = previewChartTitles,
        state = ViewState.Success(cluster),
        cluster = cluster
    )
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun TopChartsBodyErrorPreview() {
    TopChartsBody(
        selectedIndex = 0,
        chartTitles = previewChartTitles,
        state = ViewState.Error("Network error"),
        cluster = null
    )
}
