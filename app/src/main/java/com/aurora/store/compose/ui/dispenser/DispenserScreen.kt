/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.dispenser

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurora.extensions.copyToClipBoard
import com.aurora.store.R
import com.aurora.store.compose.composable.DispenserListItem
import com.aurora.store.compose.composable.Error
import com.aurora.store.compose.composable.TopAppBar
import com.aurora.store.compose.preview.PreviewTemplate
import com.aurora.store.viewmodel.dispenser.DispenserViewModel

@Composable
fun DispenserScreen(onNavigateUp: () -> Unit, viewModel: DispenserViewModel = hiltViewModel()) {
    val dispensers by viewModel.dispensers.collectAsStateWithLifecycle()

    var shouldShowInputDialog by rememberSaveable { mutableStateOf(false) }
    if (shouldShowInputDialog) {
        InputDispenserDialog(
            onAdd = { url ->
                viewModel.addDispenser(url)
                shouldShowInputDialog = false
            },
            onDismiss = { shouldShowInputDialog = false }
        )
    }

    var shouldRemoveDispenser: String? by rememberSaveable { mutableStateOf(null) }
    shouldRemoveDispenser?.let { url ->
        RemoveDispenserDialog(
            url = url,
            onRemove = {
                viewModel.removeDispenser(url)
                shouldRemoveDispenser = null
            },
            onDismiss = { shouldRemoveDispenser = null }
        )
    }

    ScreenContent(
        dispensers = dispensers,
        onNavigateUp = onNavigateUp,
        onAddDispenser = { shouldShowInputDialog = true },
        onRemoveDispenser = { url -> shouldRemoveDispenser = url }
    )
}

@Composable
private fun ScreenContent(
    onNavigateUp: () -> Unit = {},
    dispensers: Set<String> = emptySet(),
    onAddDispenser: () -> Unit = {},
    onRemoveDispenser: (url: String) -> Unit = {}
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.pref_dispenser_title),
                onNavigateUp = onNavigateUp
            )
        },
        floatingActionButton = {
            if (dispensers.isNotEmpty()) {
                FloatingActionButton(onClick = onAddDispenser) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = stringResource(R.string.add_dispenser_title)
                    )
                }
            }
        }
    ) { paddingValues ->
        if (dispensers.isEmpty()) {
            Error(
                modifier = Modifier.padding(paddingValues),
                painter = painterResource(R.drawable.ic_server),
                message = stringResource(R.string.no_dispensers_available),
                actionMessage = stringResource(R.string.add_dispenser_title),
                onAction = onAddDispenser
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(vertical = dimensionResource(R.dimen.padding_medium))
            ) {
                items(items = dispensers.toList(), key = { url -> url }) { url ->
                    DispenserListItem(
                        url = url,
                        onClick = { context.copyToClipBoard(url) },
                        onClear = { onRemoveDispenser(url) }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun DispenserScreenPreview() {
    val dispensers = List(10) { "https://auroraoss.com/api/auth/$it" }
    PreviewTemplate {
        ScreenContent(dispensers = dispensers.toSet())
    }
}

@Preview
@Composable
private fun DispenserScreenEmptyPreview() {
    PreviewTemplate {
        ScreenContent()
    }
}
