/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.menu

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.aurora.store.R

/**
 * Menu for the blacklist screen
 * @param onMenuItemClicked Callback when a menu item has been clicked
 * @see MenuItem
 */
@Composable
fun BlacklistMenu(onMenuItemClicked: (menuItem: MenuItem) -> Unit = {}) {
    var expanded by remember { mutableStateOf(false) }
    fun onClick(menuItem: MenuItem) {
        onMenuItemClicked(menuItem)
        expanded = false
    }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.action_select_all)) },
                onClick = { onClick(MenuItem.SELECT_ALL) }
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.action_remove_all)) },
                onClick = { onClick(MenuItem.REMOVE_ALL) }
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.action_import)) },
                onClick = { onClick(MenuItem.IMPORT) }
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.action_export)) },
                onClick = { onClick(MenuItem.EXPORT) }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BlacklistMenuPreview() {
    BlacklistMenu()
}
