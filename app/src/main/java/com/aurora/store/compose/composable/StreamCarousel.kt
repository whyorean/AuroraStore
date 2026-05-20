/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.StreamBundle
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.store.R
import com.aurora.store.compose.composable.app.AppListItem
import com.aurora.store.compose.composable.app.LargeAppListItem
import com.aurora.store.compose.preview.ThemePreviewProvider
import kotlinx.coroutines.flow.distinctUntilChanged

private const val LOAD_MORE_THRESHOLD = 2

@Composable
fun StreamCarousel(
    modifier: Modifier = Modifier,
    streamBundle: StreamBundle?,
    filterSingleAppClusters: Boolean = true,
    lazyListState: LazyListState = rememberLazyListState(),
    onHeaderClick: (StreamCluster) -> Unit = {},
    onAppClick: (App) -> Unit = {},
    onClusterScrolled: (StreamCluster) -> Unit = {},
    onScrolledToEnd: () -> Unit = {}
) {
    val bundleLoaded = streamBundle != null
    LaunchedEffect(lazyListState, bundleLoaded) {
        snapshotFlow {
            val last = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = lazyListState.layoutInfo.totalItemsCount
            last >= total - LOAD_MORE_THRESHOLD
        }.distinctUntilChanged().collect { reachedEnd ->
            if (reachedEnd && bundleLoaded) onScrolledToEnd()
        }
    }

    if (streamBundle == null) {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            state = lazyListState,
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
        ) {
            items(5) { ShimmerCarouselSection() }
        }
        return
    }

    val clusters = streamBundle.streamClusters.values
        .filter { cluster ->
            cluster.clusterAppList.isNotEmpty() &&
                cluster.clusterTitle.isNotBlank() &&
                (!filterSingleAppClusters || cluster.clusterAppList.size > 1)
        }

    if (clusters.isEmpty()) {
        Placeholder(
            modifier = modifier,
            painter = painterResource(R.drawable.ic_apps),
            message = stringResource(R.string.no_apps_available)
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = lazyListState,
        verticalArrangement = Arrangement.spacedBy(
            dimensionResource(
                if (clusters.size == 1) {
                    R.dimen.spacing_medium
                } else {
                    R.dimen.spacing_xsmall
                }
            )
        )
    ) {
        if (clusters.size == 1) {
            val apps = clusters.first().clusterAppList
            items(count = apps.size, key = { apps[it].id }) { index ->
                LargeAppListItem(
                    app = apps[index],
                    onClick = { onAppClick(apps[index]) }
                )
            }
        } else {
            clusters.forEach { cluster ->
                item(key = "header_${cluster.id}") {
                    SectionHeader(
                        title = cluster.clusterTitle,
                        onClick = if (cluster.clusterBrowseUrl.isNotBlank()) {
                            { onHeaderClick(cluster) }
                        } else {
                            null
                        }
                    )
                }
                item(key = "row_${cluster.id}") {
                    ClusterRow(
                        cluster = cluster,
                        onAppClick = onAppClick,
                        onClusterScrolled = onClusterScrolled
                    )
                }
            }
        }

        if (streamBundle.hasNext()) {
            item(key = "shimmer_footer") { ShimmerCarouselSection() }
        }
    }
}

@Composable
internal fun ClusterRow(
    cluster: StreamCluster,
    onAppClick: (App) -> Unit = {},
    onClusterScrolled: (StreamCluster) -> Unit = {}
) {
    val rowState = rememberLazyListState()
    val reachedEnd by remember {
        derivedStateOf {
            val last = rowState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = rowState.layoutInfo.totalItemsCount
            last >= total - LOAD_MORE_THRESHOLD
        }
    }

    LaunchedEffect(reachedEnd) {
        if (reachedEnd && cluster.hasNext()) onClusterScrolled(cluster)
    }

    LazyRow(
        state = rowState,
        contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.spacing_small)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
    ) {
        itemsIndexed(
            items = cluster.clusterAppList,
            key = { _, app -> app.packageName }
        ) { _, app ->
            AppListItem(
                app = app,
                onClick = { onAppClick(app) }
            )
        }
        if (cluster.hasNext()) {
            item(key = "shimmer_${cluster.id}") { ShimmerAppListItem() }
        }
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun StreamCarouselLoadingPreview() {
    StreamCarousel(streamBundle = null)
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun StreamCarouselEmptyPreview() {
    StreamCarousel(streamBundle = StreamBundle.EMPTY)
}
