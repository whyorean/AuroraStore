/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.home.menu

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.aurora.store.R
import com.aurora.store.compose.preview.PreviewTemplate

/**
 * Menu for home container screen
 * @param modifier Modifier for the composable
 * @param onMenuItemClicked Callback when a menu action is clicked
 */
@Composable
fun HomeContainerMenu(
    modifier: Modifier = Modifier,
    onMenuItemClicked: (menuItem: MenuItem) -> Unit = {}
) {
    IconButton(onClick = { onMenuItemClicked(MenuItem.DOWNLOADS) }) {
        Icon(
            painter = painterResource(R.drawable.ic_download_manager),
            contentDescription = stringResource(R.string.title_download_manager)
        )
    }

    IconButton(onClick = { onMenuItemClicked(MenuItem.MORE) }) {
        Icon(
            painter = painterResource(R.drawable.ic_settings_account),
            contentDescription = stringResource(R.string.title_more)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeContainerMenuPreview() {
    PreviewTemplate {
        HomeContainerMenu()
    }
}
