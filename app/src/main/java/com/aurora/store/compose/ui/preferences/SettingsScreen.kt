/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.preferences

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.aurora.store.R
import com.aurora.store.compose.composable.TopAppBar
import com.aurora.store.compose.navigation.Destination
import com.aurora.store.compose.preview.ThemePreviewProvider
import com.aurora.store.data.model.PermissionType

@Composable
fun SettingsScreen(onNavigateTo: (Destination) -> Unit) {
    ScreenContent(onNavigateTo = onNavigateTo)
}

@Composable
private fun ScreenContent(onNavigateTo: (Destination) -> Unit = {}) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.title_settings)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            item {
                ListItem(
                    modifier = Modifier.clickable {
                        onNavigateTo(
                            Destination.PermissionRationale(PermissionType.entries.toSet())
                        )
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_list_check),
                            contentDescription = null
                        )
                    },
                    headlineContent = {
                        Text(stringResource(R.string.onboarding_title_permissions))
                    }
                )
            }
            item {
                ListItem(
                    modifier = Modifier.clickable {
                        onNavigateTo(Destination.InstallationPreference)
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_installation),
                            contentDescription = null
                        )
                    },
                    headlineContent = { Text(stringResource(R.string.title_installation)) }
                )
            }
            item {
                ListItem(
                    modifier = Modifier.clickable { onNavigateTo(Destination.UIPreference) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_ui),
                            contentDescription = null
                        )
                    },
                    headlineContent = { Text(stringResource(R.string.pref_ui_title)) }
                )
            }
            item {
                ListItem(
                    modifier = Modifier.clickable { onNavigateTo(Destination.NetworkPreference) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_network),
                            contentDescription = null
                        )
                    },
                    headlineContent = { Text(stringResource(R.string.pref_network_title)) }
                )
            }
            item {
                ListItem(
                    modifier = Modifier.clickable { onNavigateTo(Destination.UpdatesPreference) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_updates),
                            contentDescription = null
                        )
                    },
                    headlineContent = { Text(stringResource(R.string.title_updates)) }
                )
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
