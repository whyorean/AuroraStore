/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import com.aurora.extensions.browse
import com.aurora.store.R
import com.aurora.store.compose.preview.ThemePreviewProvider
import com.aurora.store.data.model.ExodusTracker

/**
 * Composable to display details about a tracker reported by Exodus Privacy
 * @param modifier The modifier to be applied to the composable
 * @param tracker Tracker to display details about
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExodusListItem(modifier: Modifier = Modifier, tracker: ExodusTracker) {
    val context = LocalContext.current
    ListItem(
        onClick = { context.browse(tracker.url) },
        modifier = modifier.fillMaxWidth(),
        enabled = tracker.url.isNotBlank(),
        shapes = com.aurora.store.compose.composable.auroraListItemShapes(),
        supportingContent = {
            Column {
                if (tracker.categories.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = dimensionResource(R.dimen.spacing_xsmall))
                    ) {
                        tracker.categories.forEach { category -> CategoryChip(category) }
                    }
                }
                Text(
                    text = tracker.signature,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = dimensionResource(R.dimen.spacing_xsmall))
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(
            horizontal = dimensionResource(R.dimen.spacing_large),
            vertical = dimensionResource(R.dimen.spacing_small)
        )
    ) {
        Text(
            text = tracker.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.W400,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CategoryChip(category: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = category,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(
                horizontal = dimensionResource(R.dimen.spacing_small),
                vertical = 2.dp
            )
        )
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun ExodusListItemPreview() {
    ExodusListItem(
        tracker = ExodusTracker(
            name = "Google Analytics",
            signature = "com.google.android.apps.analytics.|com.google.analytics.",
            date = "2017-09-24",
            url = "http://www.google.com/analytics/",
            categories = listOf("Analytics", "Advertisement")
        )
    )
}
