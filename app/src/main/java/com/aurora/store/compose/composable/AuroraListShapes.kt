/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.aurora.store.R

/** Shared one-off list interaction shape matching the home app-card radius. */
@Composable
fun auroraListItemShapes(): ListItemShapes {
    val shape = RoundedCornerShape(dimensionResource(R.dimen.radius_large))
    return listItemShapes(shape)
}

/** M3 segmented rows round only the outside top and bottom edges, like the home app cards. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun auroraSegmentedListItemShapes(index: Int, count: Int): ListItemShapes {
    val radius = dimensionResource(R.dimen.radius_large)
    val shape = when {
        count <= 1 -> RoundedCornerShape(radius)
        index <= 0 -> RoundedCornerShape(topStart = radius, topEnd = radius)
        index >= count - 1 -> RoundedCornerShape(bottomStart = radius, bottomEnd = radius)
        else -> RoundedCornerShape(0.dp)
    }
    return listItemShapes(shape)
}

@Composable
private fun listItemShapes(shape: RoundedCornerShape): ListItemShapes = ListItemDefaults.shapes(
    shape = shape,
    selectedShape = shape,
    pressedShape = shape,
    focusedShape = shape,
    hoveredShape = shape,
    draggedShape = shape
)
