/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.aurora.store.compose.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CheckableDropdownMenuItem
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp

/** A popup container for Material 3 Expressive menu groups. */
@Composable
fun ExpressiveMenuPopup(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    DropdownMenuPopup(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MenuDefaults.GroupSpacing)) {
            content()
        }
    }
}

/** A visually distinct group within an expressive menu popup. */
@Composable
fun ExpressiveMenuGroup(
    index: Int,
    count: Int,
    content: @Composable ColumnScope.() -> Unit
) {
    DropdownMenuGroup(
        shapes = MenuDefaults.groupShape(index, count),
        content = content
    )
}

/** A standard expressive menu action with the shape for its position in a group. */
@Composable
fun ExpressiveMenuItem(
    index: Int,
    count: Int,
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true
) {
    DropdownMenuItem(
        onClick = onClick,
        text = text,
        shape = menuItemShape(index, count),
        modifier = modifier,
        leadingIcon = leadingIcon,
        enabled = enabled
    )
}

/** A single-selection expressive menu item with the native selected color and shape motion. */
@Composable
fun ExpressiveSelectableMenuItem(
    index: Int,
    count: Int,
    selected: Boolean,
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedLeadingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true
) {
    SelectableDropdownMenuItem(
        selected = selected,
        onClick = onClick,
        text = text,
        shapes = MenuDefaults.itemShape(index, count),
        modifier = modifier,
        selectedLeadingIcon = selectedLeadingIcon,
        enabled = enabled,
        colors = MenuDefaults.selectableItemVibrantColors()
    )
}

/** A toggleable expressive menu item with the native selected color and shape motion. */
@Composable
fun ExpressiveCheckableMenuItem(
    index: Int,
    count: Int,
    checked: Boolean,
    text: @Composable () -> Unit,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    checkedLeadingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true
) {
    CheckableDropdownMenuItem(
        checked = checked,
        onCheckedChange = onCheckedChange,
        text = text,
        shapes = MenuDefaults.itemShape(index, count),
        modifier = modifier,
        leadingIcon = leadingIcon,
        checkedLeadingIcon = checkedLeadingIcon,
        enabled = enabled,
        colors = MenuDefaults.selectableItemVibrantColors()
    )
}

@Composable
private fun menuItemShape(index: Int, count: Int): Shape = when {
    count <= 1 -> MenuDefaults.standaloneItemShape
    index == 0 -> MenuDefaults.leadingItemShape
    index == count - 1 -> MenuDefaults.trailingItemShape
    else -> MenuDefaults.middleItemShape
}

/** A menu leading icon with the standard Material menu icon size. */
@Composable
fun ExpressiveMenuIcon(
    painter: Painter,
    contentDescription: String? = null
) {
    Icon(
        painter = painter,
        contentDescription = contentDescription,
        modifier = Modifier.size(24.dp)
    )
}
