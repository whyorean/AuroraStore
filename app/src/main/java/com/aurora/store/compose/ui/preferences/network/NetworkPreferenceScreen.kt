/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.preferences.network

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aurora.store.R
import com.aurora.store.compose.composable.TopAppBar
import com.aurora.store.compose.navigation.Destination
import com.aurora.store.compose.preview.ThemePreviewProvider
import com.aurora.store.compose.ui.commons.ForceRestartDialog
import com.aurora.store.util.CommonUtil
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_MICROG_AUTH
import com.aurora.store.util.Preferences.PREFERENCE_PROXY_INFO
import com.aurora.store.util.Preferences.PREFERENCE_PROXY_URL
import com.aurora.store.util.Preferences.PREFERENCE_VENDING_VERSION
import com.aurora.store.util.remove
import com.aurora.store.util.save
import com.aurora.store.viewmodel.preferences.ProxyURLViewModel
import com.jakewharton.processphoenix.ProcessPhoenix

@Composable
fun NetworkPreferenceScreen(
    onNavigateTo: (Destination) -> Unit,
    viewModel: ProxyURLViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val hasMicroG = PackageUtil.hasSupportedMicroGVariant(context)

    ScreenContent(
        onNavigateTo = onNavigateTo,
        hasMicroG = hasMicroG,
        onSaveProxyUrl = { url ->
            val proxyInfo = CommonUtil.parseProxyUrl(url)
            if (proxyInfo != null) {
                context.save(PREFERENCE_PROXY_URL, url)
                context.save(PREFERENCE_PROXY_INFO, viewModel.json.encodeToString(proxyInfo))
                true
            } else {
                false
            }
        },
        onDeleteProxyUrl = {
            Preferences.remove(context, PREFERENCE_PROXY_URL)
            Preferences.remove(context, PREFERENCE_PROXY_INFO)
        }
    )
}

@Composable
private fun ScreenContent(
    onNavigateTo: (Destination) -> Unit = {},
    hasMicroG: Boolean = false,
    onSaveProxyUrl: (String) -> Boolean = { false },
    onDeleteProxyUrl: () -> Unit = {}
) {
    val context = LocalContext.current

    val vendingEntries = stringArrayResource(R.array.pref_vending_version)
    var vendingVersion by remember {
        mutableIntStateOf(Preferences.getInteger(context, PREFERENCE_VENDING_VERSION))
    }
    var microGAuth by remember {
        mutableStateOf(Preferences.getBoolean(context, PREFERENCE_MICROG_AUTH, true))
    }
    var showProxyDialog by remember { mutableStateOf(false) }
    var showVendingDialog by remember { mutableStateOf(false) }
    var showForceRestartDialog by remember { mutableStateOf(false) }

    if (showForceRestartDialog) {
        ForceRestartDialog(
            onConfirm = { ProcessPhoenix.triggerRebirth(context) },
            onDismiss = { showForceRestartDialog = false }
        )
    }

    if (showProxyDialog) {
        ProxyURLDialog(
            currentUrl = Preferences.getString(context, PREFERENCE_PROXY_URL),
            onSave = { url ->
                showProxyDialog = false
                if (onSaveProxyUrl(url)) {
                    showForceRestartDialog = true
                }
            },
            onDelete = {
                showProxyDialog = false
                onDeleteProxyUrl()
                showForceRestartDialog = true
            },
            onDismiss = { showProxyDialog = false }
        )
    }

    if (showVendingDialog) {
        SingleChoiceDialog(
            title = stringResource(R.string.pref_vending_version_title),
            options = vendingEntries.toList(),
            selected = vendingVersion,
            onSelect = { index ->
                vendingVersion = index
                context.save(PREFERENCE_VENDING_VERSION, index)
                showVendingDialog = false
            },
            onDismiss = { showVendingDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.pref_network_title)
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
                    modifier = Modifier.clickable { onNavigateTo(Destination.Dispenser) },
                    headlineContent = { Text(stringResource(R.string.pref_dispenser_title)) },
                    supportingContent = { Text(stringResource(R.string.pref_dispenser_summary)) }
                )
            }
            item {
                ListItem(
                    modifier = Modifier.clickable { showProxyDialog = true },
                    headlineContent = { Text(stringResource(R.string.pref_network_proxy_url)) },
                    supportingContent = { Text(stringResource(R.string.pref_network_proxy_desc)) }
                )
            }
            item { HorizontalDivider() }
            item {
                ListItem(headlineContent = { Text(stringResource(R.string.pref_common_extra)) })
            }
            if (hasMicroG) {
                item {
                    ListItem(
                        modifier = Modifier.clickable {
                            microGAuth = !microGAuth
                            context.save(PREFERENCE_MICROG_AUTH, microGAuth)
                        },
                        headlineContent = {
                            Text(stringResource(R.string.pref_network_microg_login_title))
                        },
                        supportingContent = {
                            Text(stringResource(R.string.pref_network_microg_login_desc))
                        },
                        trailingContent = {
                            Switch(
                                checked = microGAuth,
                                onCheckedChange = { checked ->
                                    microGAuth = checked
                                    context.save(PREFERENCE_MICROG_AUTH, checked)
                                }
                            )
                        }
                    )
                }
            }
            item {
                ListItem(
                    modifier = Modifier.clickable { showVendingDialog = true },
                    headlineContent = { Text(stringResource(R.string.pref_vending_version_title)) },
                    supportingContent = { Text(vendingEntries.getOrElse(vendingVersion) { "" }) }
                )
            }
        }
    }
}

@Composable
private fun ProxyURLDialog(
    currentUrl: String,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var url by remember { mutableStateOf(currentUrl) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pref_network_proxy_url)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.pref_network_proxy_url_message),
                    modifier = Modifier.padding(bottom = dimensionResource(R.dimen.padding_small))
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.pref_network_proxy_url_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = url.isNotBlank(),
                onClick = { onSave(url.trim()) }
            ) {
                Text(stringResource(R.string.set))
            }
        },
        dismissButton = {
            Row {
                if (currentUrl.isNotBlank()) {
                    TextButton(onClick = onDelete) {
                        Text(stringResource(R.string.disable))
                    }
                }
                Spacer(Modifier.width(dimensionResource(R.dimen.margin_small)))
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        }
    )
}

@Composable
internal fun SingleChoiceDialog(
    title: String,
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                options.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = index == selected,
                                onClick = { onSelect(index) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = dimensionResource(R.dimen.padding_small)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = index == selected,
                            onClick = null
                        )
                        Spacer(Modifier.width(dimensionResource(R.dimen.margin_small)))
                        Text(option)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview
@Composable
private fun NetworkPreferenceScreenPreview() {
    ScreenContent(hasMicroG = true)
}
