/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.tv.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.tv.material3.Text
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.StreamBundle
import com.aurora.store.R
import com.aurora.store.tv.ui.commons.AppCard

@Composable
fun StreamRows(
    streamBundle: StreamBundle?,
    onAppClick: (App) -> Unit,
    modifier: Modifier = Modifier
) {
    // Drop untitled clusters, matching the phone StreamCarousel which only shows titled rows.
    val clusters = streamBundle?.streamClusters?.values
        ?.filter { it.clusterTitle.isNotBlank() }
        .orEmpty()
    // Use contentPadding (not Modifier.padding) so a focused card's scaled-up edges grow into the
    // padding — which is inside the list's clip bounds — instead of being clipped at the edge or
    // drawn under the navigation drawer.
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = dimensionResource(R.dimen.spacing_xlarge)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.tv_row_spacing))
    ) {
        items(items = clusters, key = { it.id }) { cluster ->
            Text(
                text = cluster.clusterTitle,
                modifier = Modifier.padding(
                    start = dimensionResource(R.dimen.spacing_xlarge),
                    bottom = dimensionResource(R.dimen.spacing_small)
                )
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(
                    dimensionResource(R.dimen.spacing_medium)
                ),
                contentPadding = PaddingValues(
                    horizontal = dimensionResource(R.dimen.spacing_xlarge)
                )
            ) {
                items(items = cluster.clusterAppList, key = { it.packageName }) { app ->
                    AppCard(app = app, onClick = { onAppClick(app) })
                }
            }
        }
    }
}
