/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composables

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction

/**
 * A top app bar composable to be used with Scaffold in different Screen
 * @param searchHint Hint to show to the user in search bar
 * @param onNavigateUp Action when user clicks the navigation icon
 * @param onSearch Callback for a search
 * @param actions Actions to display on the top app bar (for e.g. menu)
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SearchAppBarComposable(
    @StringRes searchHint: Int? = null,
    onNavigateUp: () -> Unit,
    onSearch: (query: String) -> Unit,
    actions: @Composable (RowScope.() -> Unit) = {}
) {
    val focusManager = LocalFocusManager.current
    var query by rememberSaveable { mutableStateOf("") }

    TopAppBar(
        title = {
            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = query,
                onValueChange = { newQuery ->
                    query = newQuery.trim()
                    onSearch(query)
                },
                maxLines = 1,
                placeholder = { if (searchHint != null) Text(text = stringResource(searchHint)) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateUp) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null
                )
            }
        },
        actions = {
            if (query.isNotBlank()) {
                IconButton(
                    onClick = {
                        query = ""
                        onSearch(query)
                        focusManager.clearFocus(force = true)
                    }
                ) {
                    Icon(imageVector = Icons.Default.Clear, contentDescription = null)
                }
            }
            actions()
        }
    )
}
