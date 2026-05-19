/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.aurora.store.R
import com.aurora.store.compose.preview.ThemePreviewProvider

/**
 * A top app bar composable to be used with Scaffold in different Screen
 * @param modifier The modifier to be applied to the composable
 * @param title Title of the screen
 * @param navigationIcon Icon for the navigation button
 * @param showNavigationIcon Whether to show the navigation (back) icon button
 * @param actions Actions to display on the top app bar (for e.g. menu)
 */
@Composable
fun TopAppBar(
    modifier: Modifier = Modifier,
    title: String? = null,
    navigationIcon: Painter = painterResource(R.drawable.ic_arrow_back),
    showNavigationIcon: Boolean = true,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    actions: @Composable (RowScope.() -> Unit) = {}
) {
    val activity = LocalActivity.current as? ComponentActivity
    TopAppBar(
        modifier = modifier,
        title = { if (title != null) Text(text = title) },
        navigationIcon = {
            if (showNavigationIcon) {
                IconButton(onClick = { activity?.onBackPressedDispatcher?.onBackPressed() }) {
                    Icon(
                        painter = navigationIcon,
                        contentDescription = stringResource(R.string.action_back)
                    )
                }
            }
        },
        windowInsets = windowInsets,
        actions = actions
    )
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun TopAppBarPreview() {
    TopAppBar(
        title = stringResource(R.string.title_about)
    )
}
