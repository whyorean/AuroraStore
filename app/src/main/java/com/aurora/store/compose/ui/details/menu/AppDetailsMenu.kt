/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details.menu

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
import com.aurora.store.compose.composable.TopAppBar
import com.aurora.store.compose.preview.PreviewTemplate
import com.aurora.store.data.model.AppState

/**
 * Menu for the app details screen
 * @param modifier The modifier to be applied to the composable
 * @param onMenuItemClicked Callback when a menu item has been clicked
 * @see MenuItem
 */
@Composable
fun AppDetailsMenu(
    modifier: Modifier = Modifier,
    state: AppState = AppState.Unavailable,
    isFavorite: Boolean = false,
    isExpanded: Boolean = false,
    onMenuItemClicked: (menuItem: MenuItem) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(isExpanded) }
    fun onClick(menuItem: MenuItem) {
        onMenuItemClicked(menuItem)
        expanded = false
    }

    IconButton(onClick = { onClick(MenuItem.FAVORITE) }) {
        Icon(
            painter = if (isFavorite) {
                painterResource(R.drawable.ic_favorite_checked)
            } else {
                painterResource(R.drawable.ic_favorite_unchecked)
            },
            contentDescription = stringResource(R.string.action_favourite)
        )
    }

    IconButton(onClick = { onClick(MenuItem.SHARE) }) {
        Icon(
            painter = painterResource(R.drawable.ic_share),
            contentDescription = stringResource(R.string.action_share)
        )
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
                text = { Text(text = stringResource(R.string.title_manual_download)) },
                onClick = { onClick(MenuItem.MANUAL_DOWNLOAD) },
                enabled = !state.inProgress()
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.action_info)) },
                onClick = { onClick(MenuItem.APP_INFO) },
                enabled = state is AppState.Installed || state is AppState.Updatable
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.action_home_screen)) },
                onClick = { onClick(MenuItem.ADD_TO_HOME) },
                enabled = state is AppState.Installed || state is AppState.Updatable
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppDetailsMenuPreview() {
    PreviewTemplate {
        TopAppBar(
            actions = {
                AppDetailsMenu(isFavorite = true, isExpanded = true)
            }
        )
    }
}
