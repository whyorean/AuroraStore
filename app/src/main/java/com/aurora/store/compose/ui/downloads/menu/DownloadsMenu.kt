/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.downloads.menu

import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.aurora.store.R

/**
 * Menu for the blacklist screen
 * @param modifier The modifier to be applied to the composable
 * @param onMenuItemClicked Callback when a menu item has been clicked
 * @see MenuItem
 */
@Composable
fun DownloadsMenu(
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    onMenuItemClicked: (menuItem: MenuItem) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(isExpanded) }
    fun onClick(menuItem: MenuItem) {
        onMenuItemClicked(menuItem)
        expanded = false
    }

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(R.drawable.ic_more_vert),
                contentDescription = stringResource(R.string.menu)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.download_cancel_all)) },
                onClick = { onClick(MenuItem.CANCEL_ALL) }
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.download_clear_finished)) },
                onClick = { onClick(MenuItem.CLEAR_FINISHED) }
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.download_force_clear_all)) },
                onClick = { onClick(MenuItem.FORCE_CLEAR_ALL) }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DownloadsMenuPreview() {
    DownloadsMenu(isExpanded = true)
}
