/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.preferences

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import com.aurora.store.R
import com.aurora.store.compose.composable.auroraSegmentedListItemShapes

enum class PreferenceRowPosition {
    Single,
    First,
    Middle,
    Last
}

/** A standard Material 3 settings row with shared-surface list grouping. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PreferenceListItem(
    modifier: Modifier = Modifier,
    headlineContent: @Composable () -> Unit,
    supportingContent: (@Composable () -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    position: PreferenceRowPosition = PreferenceRowPosition.Single,
    horizontalPadding: Dp = dimensionResource(R.dimen.spacing_large)
) {
    val shapeIndex: Int
    val shapeCount: Int
    when (position) {
        PreferenceRowPosition.Single -> {
            shapeIndex = 0
            shapeCount = 1
        }
        PreferenceRowPosition.First -> {
            shapeIndex = 0
            shapeCount = 2
        }
        PreferenceRowPosition.Middle -> {
            shapeIndex = 1
            shapeCount = 3
        }
        PreferenceRowPosition.Last -> {
            shapeIndex = 1
            shapeCount = 2
        }
    }
    SegmentedListItem(
        shapes = auroraSegmentedListItemShapes(shapeIndex, shapeCount),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .then(
                when (position) {
                    PreferenceRowPosition.Single -> Modifier.padding(
                        vertical = dimensionResource(R.dimen.spacing_xsmall)
                    )
                    PreferenceRowPosition.First,
                    PreferenceRowPosition.Middle -> Modifier.padding(
                        bottom = dimensionResource(R.dimen.spacing_xsmall)
                    )
                    PreferenceRowPosition.Last -> Modifier
                }
            )
            .then(modifier),
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        contentPadding = ListItemDefaults.ContentPadding,
        verticalAlignment = Alignment.CenterVertically,
        supportingContent = supportingContent,
        leadingContent = leadingContent,
        trailingContent = trailingContent
    ) {
        headlineContent()
    }
}

fun preferenceRowPosition(index: Int, count: Int): PreferenceRowPosition = when {
    count <= 1 -> PreferenceRowPosition.Single
    index <= 0 -> PreferenceRowPosition.First
    index >= count - 1 -> PreferenceRowPosition.Last
    else -> PreferenceRowPosition.Middle
}

@Composable
fun PreferenceSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.spacing_large),
                vertical = dimensionResource(R.dimen.spacing_small)
            )
    )
}
