/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.preferences.installation

import android.app.admin.DevicePolicyManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.core.content.getSystemService
import com.aurora.store.R
import com.aurora.store.compose.composable.TopAppBar
import com.aurora.store.compose.navigation.Destination
import com.aurora.store.compose.preview.ThemePreviewProvider
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_AUTO_DELETE
import com.aurora.store.util.save

@Composable
fun InstallationPreferenceScreen(onNavigateTo: (Destination) -> Unit) {
    val context = LocalContext.current
    val devicePolicyManager = context.getSystemService<DevicePolicyManager>()
    val isDeviceOwner = devicePolicyManager?.isDeviceOwnerApp(context.packageName) ?: false

    ScreenContent(
        onNavigateTo = onNavigateTo,
        isDeviceOwner = isDeviceOwner,
        onClearDeviceOwner = {
            @Suppress("DEPRECATION")
            devicePolicyManager?.clearDeviceOwnerApp(context.packageName)
        }
    )
}

@Composable
private fun ScreenContent(
    onNavigateTo: (Destination) -> Unit = {},
    isDeviceOwner: Boolean = false,
    onClearDeviceOwner: () -> Unit = {}
) {
    val context = LocalContext.current
    var autoDelete by remember {
        mutableStateOf(Preferences.getBoolean(context, PREFERENCE_AUTO_DELETE, true))
    }
    var showClearOwnerDialog by remember { mutableStateOf(false) }

    if (showClearOwnerDialog) {
        AlertDialog(
            onDismissRequest = { showClearOwnerDialog = false },
            title = { Text(stringResource(R.string.pref_clear_device_owner_title)) },
            text = { Text(stringResource(R.string.pref_clear_device_owner_desc)) },
            confirmButton = {
                TextButton(onClick = {
                    showClearOwnerDialog = false
                    onClearDeviceOwner()
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearOwnerDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.title_installation)
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
                    modifier = Modifier.clickable { onNavigateTo(Destination.Installer) },
                    headlineContent = { Text(stringResource(R.string.pref_install_mode_title)) },
                    supportingContent = { Text(stringResource(R.string.pref_install_mode_summary)) }
                )
            }
            item { HorizontalDivider() }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.pref_common_extra)) }
                )
            }
            item {
                ListItem(
                    modifier = Modifier.clickable {
                        autoDelete = !autoDelete
                        context.save(PREFERENCE_AUTO_DELETE, autoDelete)
                    },
                    headlineContent = { Text(stringResource(R.string.pref_install_delete_title)) },
                    supportingContent = {
                        Text(stringResource(R.string.pref_install_delete_summary))
                    },
                    trailingContent = {
                        Switch(
                            checked = autoDelete,
                            onCheckedChange = { checked ->
                                autoDelete = checked
                                context.save(PREFERENCE_AUTO_DELETE, checked)
                            }
                        )
                    }
                )
            }
            if (isDeviceOwner) {
                item {
                    ListItem(
                        modifier = Modifier.clickable { showClearOwnerDialog = true },
                        headlineContent = {
                            Text(stringResource(R.string.pref_clear_device_owner_title))
                        },
                        supportingContent = {
                            Text(stringResource(R.string.pref_clear_device_owner_summary))
                        }
                    )
                }
            }
        }
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview
@Composable
private fun InstallationPreferenceScreenPreview() {
    ScreenContent()
}
