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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.compose.composables.TopAppBarComposable
import com.aurora.store.compose.composables.preview.AppPreviewProvider
import com.aurora.store.compose.menu.items.AppDetailsMenuItem

/**
 * Menu for the app details screen
 * @param app App for which this menu should be inflated
 * @param modifier The modifier to be applied to the composable
 * @param onMenuItemClicked Callback when a menu item has been clicked
 * @see AppDetailsMenuItem
 */
@Composable
fun AppDetailsMenu(
    modifier: Modifier = Modifier,
    isInstalled: Boolean = false,
    isFavorite: Boolean = false,
    isExpanded: Boolean = false,
    onMenuItemClicked: (appDetailsMenuItem: AppDetailsMenuItem) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(isExpanded) }
    fun onClick(appDetailsMenuItem: AppDetailsMenuItem) {
        onMenuItemClicked(appDetailsMenuItem)
        expanded = false
    }

    IconButton(onClick = { onClick(AppDetailsMenuItem.FAVORITE) }) {
        Icon(
            painter = if (isFavorite) {
                painterResource(R.drawable.ic_favorite_checked)
            } else {
                painterResource(R.drawable.ic_favorite_unchecked)
            },
            contentDescription = stringResource(R.string.action_favourite)
        )
    }

    IconButton(onClick = { onClick(AppDetailsMenuItem.SHARE) }) {
        Icon(
            painter = painterResource(R.drawable.ic_share),
            contentDescription = stringResource(R.string.action_share)
        )
    }

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.menu))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.title_manual_download)) },
                onClick = { onClick(AppDetailsMenuItem.MANUAL_DOWNLOAD) }
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.title_download_playstore)) },
                onClick = { onClick(AppDetailsMenuItem.PLAY_STORE) }
            )

            // Inflate actions available only when app is installed below
            if (!isInstalled) return@DropdownMenu

            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.action_info)) },
                onClick = { onClick(AppDetailsMenuItem.APP_INFO) }
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.action_home_screen)) },
                onClick = { onClick(AppDetailsMenuItem.ADD_TO_HOME) }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppDetailsMenuPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    TopAppBarComposable(
        actions = {
            AppDetailsMenu(
                isInstalled = app.isInstalled,
                isFavorite = true,
                isExpanded = true
            )
        }
    )
}
