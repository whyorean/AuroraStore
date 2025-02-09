/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composables

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * A top app bar composable to be used with Scaffold in different Screen
 * @param title Title of the screen
 * @param onNavigateUp Action when user clicks the navigation icon
 * @param actions Actions to display on the top app bar (for e.g. menu)
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TopAppBarComposable(
    @StringRes title: Int? = null,
    onNavigateUp: () -> Unit,
    actions: @Composable (RowScope.() -> Unit) = {}
) {
    TopAppBar(
        title = {
            if (title != null) Text(text = stringResource(title))
        },
        navigationIcon = {
            IconButton(onClick = onNavigateUp) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null
                )
            }
        },
        actions = actions
    )
}
