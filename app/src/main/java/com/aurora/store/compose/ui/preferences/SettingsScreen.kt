/*
 * SPDX-FileCopyrightText: 2025-2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.preferences

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurora.store.R
import com.aurora.store.compose.composable.ArrowIconBox
import com.aurora.store.compose.composable.TopAppBar
import com.aurora.store.compose.composable.auroraSegmentedListItemShapes
import com.aurora.store.compose.navigation.Destination
import com.aurora.store.compose.preview.ThemePreviewProvider
import com.aurora.store.data.model.PermissionType
import kotlinx.coroutines.launch

private data class SettingEntry(
    val title: String,
    val icon: Int,
    val destination: Destination,
    val searchTerms: String
)

@Composable
fun SettingsScreen(onNavigateTo: (Destination) -> Unit) {
    ScreenContent(onNavigateTo = onNavigateTo)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ScreenContent(onNavigateTo: (Destination) -> Unit = {}) {
    val entries = listOf(
        SettingEntry(
            title = stringResource(R.string.onboarding_title_permissions),
            icon = R.drawable.ic_list_check,
            destination = Destination.PermissionRationale(PermissionType.entries.toSet()),
            searchTerms = "permissions access install storage"
        ),
        SettingEntry(
            title = stringResource(R.string.title_installation),
            icon = R.drawable.ic_installation,
            destination = Destination.InstallationPreference,
            searchTerms = "installer install apk delete"
        ),
        SettingEntry(
            title = stringResource(R.string.pref_ui_title),
            icon = R.drawable.ic_ui,
            destination = Destination.UIPreference,
            searchTerms = "language layout tabs for you apps games"
        ),
        SettingEntry(
            title = stringResource(R.string.title_notifications),
            icon = R.drawable.ic_notification_settings,
            destination = Destination.NotificationPreference,
            searchTerms = "alerts downloads progress channels"
        ),
        SettingEntry(
            title = stringResource(R.string.pref_network_title),
            icon = R.drawable.ic_network,
            destination = Destination.NetworkPreference,
            searchTerms = "proxy dispenser microg vending"
        ),
        SettingEntry(
            title = stringResource(R.string.title_updates),
            icon = R.drawable.ic_updates,
            destination = Destination.UpdatesPreference,
            searchTerms = "automatic frequency sources fdroid trackers"
        ),
        SettingEntry(
            title = stringResource(R.string.title_security),
            icon = R.drawable.ic_lock,
            destination = Destination.SecurityPreference,
            searchTerms = "app lock deeplink security"
        )
    )
    val textFieldState = rememberTextFieldState()
    val searchBarState = rememberSearchBarState()
    val coroutineScope = rememberCoroutineScope()
    val query by remember(textFieldState) {
        snapshotFlow { textFieldState.text.toString().trim() }
    }.collectAsStateWithLifecycle(initialValue = "")
    val matchingEntries = remember(query, entries) {
        if (query.isEmpty()) entries else entries.filter { entry ->
            entry.title.contains(query, ignoreCase = true) ||
                entry.searchTerms.contains(query, ignoreCase = true)
        }
    }

    val inputField: @Composable () -> Unit = {
        SearchBarDefaults.InputField(
            searchBarState = searchBarState,
            textFieldState = textFieldState,
            onSearch = {
                coroutineScope.launch { searchBarState.animateToCollapsed() }
            },
            placeholder = { Text(stringResource(R.string.pref_settings_search)) },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_round_search),
                    contentDescription = null
                )
            },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = textFieldState::clearText) {
                        Icon(
                            painter = painterResource(R.drawable.ic_cancel),
                            contentDescription = stringResource(R.string.action_clear)
                        )
                    }
                }
            }
        )
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar()
                    Text(
                        text = stringResource(R.string.title_settings),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = dimensionResource(R.dimen.spacing_large),
                                vertical = dimensionResource(R.dimen.spacing_small)
                            )
                    )
                    SearchBar(
                        state = searchBarState,
                        inputField = inputField,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dimensionResource(R.dimen.spacing_large))
                            .padding(bottom = dimensionResource(R.dimen.spacing_medium))
                    )
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    bottom = dimensionResource(R.dimen.spacing_large)
                )
            ) {
                itemsIndexed(entries, key = { _, entry -> entry.title }) { index, entry ->
                    SegmentedListItem(
                        shapes = auroraSegmentedListItemShapes(index, entries.size),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateTo(entry.destination) }
                            .padding(horizontal = dimensionResource(R.dimen.spacing_large))
                            .then(
                                if (index < entries.lastIndex) {
                                    Modifier.padding(bottom = dimensionResource(R.dimen.spacing_xsmall))
                                } else {
                                    Modifier
                                }
                            ),
                        colors = ListItemDefaults.segmentedColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        contentPadding = ListItemDefaults.ContentPadding,
                        verticalAlignment = Alignment.CenterVertically,
                        leadingContent = {
                            Icon(
                                painter = painterResource(entry.icon),
                                contentDescription = null
                            )
                        },
                        trailingContent = {
                            ArrowIconBox(
                                painter = painterResource(R.drawable.ic_arrow_right),
                                contentDescription = null,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    ) {
                        Text(entry.title)
                    }
                }
            }
        }

        ExpandedFullScreenSearchBar(state = searchBarState, inputField = inputField) {
            if (matchingEntries.isEmpty()) {
                Text(
                    text = stringResource(R.string.pref_settings_search_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        horizontal = dimensionResource(R.dimen.spacing_large),
                        vertical = dimensionResource(R.dimen.spacing_medium)
                    )
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = dimensionResource(R.dimen.spacing_small))
                ) {
                    itemsIndexed(matchingEntries, key = { _, entry -> entry.title }) { index, entry ->
                        SegmentedListItem(
                            shapes = auroraSegmentedListItemShapes(index, matchingEntries.size),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    coroutineScope.launch {
                                        searchBarState.animateToCollapsed()
                                        textFieldState.clearText()
                                        onNavigateTo(entry.destination)
                                    }
                                }
                                .padding(horizontal = dimensionResource(R.dimen.spacing_large))
                                .then(
                                    if (index < matchingEntries.lastIndex) {
                                        Modifier.padding(
                                            bottom = dimensionResource(R.dimen.spacing_xsmall)
                                        )
                                    } else {
                                        Modifier
                                    }
                                ),
                            colors = ListItemDefaults.segmentedColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            contentPadding = ListItemDefaults.ContentPadding,
                            verticalAlignment = Alignment.CenterVertically,
                            leadingContent = {
                                Icon(
                                    painter = painterResource(entry.icon),
                                    contentDescription = null
                                )
                            },
                            trailingContent = {
                                ArrowIconBox(
                                    painter = painterResource(R.drawable.ic_arrow_right),
                                    contentDescription = null,
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        ) {
                            Text(entry.title)
                        }
                    }
                }
            }
        }
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview
@Composable
private fun SettingsScreenPreview() {
    ScreenContent()
}
