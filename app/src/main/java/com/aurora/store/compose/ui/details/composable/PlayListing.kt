/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import coil3.compose.AsyncImage
import com.aurora.gplayapi.model.App
import com.aurora.gplayapi.model.Category
import com.aurora.gplayapi.model.TopChart
import com.aurora.store.R
import com.aurora.store.compose.composable.SectionHeader

@Composable
fun PlayListing(
    app: App,
    categoryIconUrl: String? = null,
    onCategoryClick: (Category) -> Unit = {},
    onTopChartClick: (chart: TopChart, title: String) -> Unit = { _, _ -> },
    onBrowse: (title: String, browseUrl: String) -> Unit = { _, _ -> }
) {
    val categoryUrl = app.categoryStreamUrl.orEmpty()
    val topChart = app.topChart?.takeIf { it.title.isNotBlank() }
    if (app.categoryName.isBlank() && topChart == null && app.deviceSurfaces.isEmpty()) return

    SectionHeader(title = stringResource(R.string.details_play_listing))
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.spacing_medium)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
    ) {
        if (app.categoryName.isNotBlank()) {
            ListingChip(
                label = app.categoryName,
                leadingIcon = categoryIconUrl?.takeIf { it.isNotBlank() }?.let { url ->
                    {
                        AsyncImage(
                            modifier = Modifier.size(AssistChipDefaults.IconSize),
                            model = url,
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                        )
                    }
                },
                onClick = if (categoryUrl.isNotBlank()) {
                    { onCategoryClick(Category(title = app.categoryName, browseUrl = categoryUrl)) }
                } else {
                    null
                }
            )
        }
        topChart?.let { chart ->
            val label = if (app.categoryName.isNotBlank()) {
                stringResource(R.string.details_top_chart, chart.title, app.categoryName)
            } else {
                chart.title
            }
            ListingChip(
                label = label,
                onClick = { onTopChartClick(chart, label) }
            )
        }
        app.deviceSurfaces.filter { it.name.isNotBlank() }.forEach { surface ->
            ListingChip(
                label = surface.name,
                onClick = if (surface.browseUrl.isNotBlank()) {
                    { onBrowse(surface.name, surface.browseUrl) }
                } else {
                    null
                }
            )
        }
    }
}

@Composable
private fun ListingChip(
    label: String,
    leadingIcon: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)?
) {
    AssistChip(
        onClick = { onClick?.invoke() },
        enabled = true,
        label = { Text(text = label, style = MaterialTheme.typography.bodySmall) },
        leadingIcon = leadingIcon
    )
}
