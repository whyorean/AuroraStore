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
import androidx.compose.material3.HorizontalDivider
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
import com.aurora.store.compose.composables.TransparentIconComposable
import com.aurora.store.compose.menu.items.BlacklistMenuItem

/**
 * Menu for the blacklist screen
 * @param modifier The modifier to be applied to the composable
 * @param onMenuItemClicked Callback when a menu item has been clicked
 * @see BlacklistMenuItem
 */
@Composable
fun BlacklistMenu(
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    onMenuItemClicked: (blacklistMenuItem: BlacklistMenuItem) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(isExpanded) }
    fun onClick(blacklistMenuItem: BlacklistMenuItem) {
        onMenuItemClicked(blacklistMenuItem)
        expanded = false
    }

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.menu))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.action_select_all)) },
                onClick = { onClick(BlacklistMenuItem.SELECT_ALL) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_select_all),
                        contentDescription = null
                    )
                }
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.action_remove_all)) },
                onClick = { onClick(BlacklistMenuItem.REMOVE_ALL) },
                leadingIcon = { TransparentIconComposable() }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.action_import)) },
                onClick = { onClick(BlacklistMenuItem.IMPORT) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_file_import),
                        contentDescription = null
                    )
                }
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.action_export)) },
                onClick = { onClick(BlacklistMenuItem.EXPORT) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_file_export),
                        contentDescription = null
                    )
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BlacklistMenuPreview() {
    BlacklistMenu(isExpanded = true)
}
