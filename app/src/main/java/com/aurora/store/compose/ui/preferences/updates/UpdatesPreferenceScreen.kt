/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.preferences.updates

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aurora.extensions.isIgnoringBatteryOptimizations
import com.aurora.extensions.isTAndAbove
import com.aurora.store.R
import com.aurora.store.compose.composable.TopAppBar
import com.aurora.store.compose.preview.ThemePreviewProvider
import com.aurora.store.compose.ui.preferences.network.SingleChoiceDialog
import com.aurora.store.data.model.UpdateMode
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCES_UPDATES_RESTRICTIONS_BATTERY
import com.aurora.store.util.Preferences.PREFERENCES_UPDATES_RESTRICTIONS_IDLE
import com.aurora.store.util.Preferences.PREFERENCES_UPDATES_RESTRICTIONS_METERED
import com.aurora.store.util.Preferences.PREFERENCE_FILTER_AURORA_ONLY
import com.aurora.store.util.Preferences.PREFERENCE_FILTER_FDROID
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_AUTO
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_CHECK_INTERVAL
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_EXTENDED
import com.aurora.store.util.save
import com.aurora.store.viewmodel.all.UpdatesViewModel
import kotlin.math.roundToInt

@Composable
fun UpdatesPreferenceScreen(viewModel: UpdatesViewModel = hiltViewModel()) {
    ScreenContent(
        onCancelAutomatedCheck = { viewModel.updateHelper.cancelAutomatedCheck() },
        onScheduleAutomatedCheck = { viewModel.updateHelper.scheduleAutomatedCheck() },
        onUpdateAutomatedCheck = { viewModel.updateHelper.updateAutomatedCheck() },
        onCheckUpdatesNow = { viewModel.updateHelper.checkUpdatesNow() }
    )
}

@Composable
private fun ScreenContent(
    onCancelAutomatedCheck: () -> Unit = {},
    onScheduleAutomatedCheck: () -> Unit = {},
    onUpdateAutomatedCheck: () -> Unit = {},
    onCheckUpdatesNow: () -> Unit = {}
) {
    val context = LocalContext.current
    val autoEntries = stringArrayResource(R.array.pref_updates_auto)

    var autoMode by remember {
        mutableIntStateOf(Preferences.getInteger(context, PREFERENCE_UPDATES_AUTO, 2))
    }
    val autoEnabled = autoMode != UpdateMode.DISABLED.ordinal

    var checkInterval by remember {
        mutableFloatStateOf(
            Preferences.getInteger(context, PREFERENCE_UPDATES_CHECK_INTERVAL, 3).toFloat()
        )
    }
    var filterFDroid by remember {
        mutableStateOf(Preferences.getBoolean(context, PREFERENCE_FILTER_FDROID, true))
    }
    var filterAuroraOnly by remember {
        mutableStateOf(Preferences.getBoolean(context, PREFERENCE_FILTER_AURORA_ONLY))
    }
    var updatesExtended by remember {
        mutableStateOf(Preferences.getBoolean(context, PREFERENCE_UPDATES_EXTENDED))
    }
    var showAutoDialog by remember { mutableStateOf(false) }
    var showRestrictionsDialog by remember { mutableStateOf(false) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            context.save(PREFERENCE_UPDATES_AUTO, UpdateMode.CHECK_AND_NOTIFY.ordinal)
            autoMode = UpdateMode.CHECK_AND_NOTIFY.ordinal
            onScheduleAutomatedCheck()
        } else {
            autoMode = Preferences.getInteger(context, PREFERENCE_UPDATES_AUTO, 2)
        }
    }

    val batteryOptLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (context.isIgnoringBatteryOptimizations()) {
            context.save(PREFERENCE_UPDATES_AUTO, UpdateMode.CHECK_AND_INSTALL.ordinal)
            autoMode = UpdateMode.CHECK_AND_INSTALL.ordinal
            onScheduleAutomatedCheck()
        } else {
            autoMode = Preferences.getInteger(context, PREFERENCE_UPDATES_AUTO, 2)
        }
    }

    if (showAutoDialog) {
        SingleChoiceDialog(
            title = stringResource(R.string.pref_updates_auto),
            options = autoEntries.toList(),
            selected = autoMode,
            onSelect = { index ->
                showAutoDialog = false
                when (UpdateMode.entries[index]) {
                    UpdateMode.DISABLED -> {
                        context.save(PREFERENCE_UPDATES_AUTO, 0)
                        autoMode = 0
                        onCancelAutomatedCheck()
                    }
                    UpdateMode.CHECK_AND_NOTIFY -> {
                        if (!isTAndAbove ||
                            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                            android.content.pm.PackageManager.PERMISSION_GRANTED
                        ) {
                            context.save(PREFERENCE_UPDATES_AUTO, 1)
                            autoMode = 1
                            onScheduleAutomatedCheck()
                        } else {
                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    UpdateMode.CHECK_AND_INSTALL -> {
                        if (context.isIgnoringBatteryOptimizations()) {
                            context.save(PREFERENCE_UPDATES_AUTO, 2)
                            autoMode = 2
                            onScheduleAutomatedCheck()
                        } else {
                            batteryOptLauncher.launch(
                                Intent(
                                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                    "package:${context.packageName}".toUri()
                                )
                            )
                        }
                    }
                    else -> {}
                }
            },
            onDismiss = { showAutoDialog = false }
        )
    }

    if (showRestrictionsDialog) {
        UpdatesRestrictionsDialog(
            onUpdateAutomatedCheck = onUpdateAutomatedCheck,
            onDismiss = { showRestrictionsDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.title_updates)
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
                    modifier = Modifier.clickable { showAutoDialog = true },
                    headlineContent = { Text(stringResource(R.string.pref_updates_auto)) },
                    supportingContent = { Text(autoEntries.getOrElse(autoMode) { "" }) }
                )
            }
            if (autoEnabled) {
                item {
                    ListItem(
                        headlineContent = {
                            Text(stringResource(R.string.pref_updates_check_frequency))
                        },
                        supportingContent = {
                            Column {
                                Text(stringResource(R.string.pref_updates_check_frequency_desc))
                                Spacer(Modifier.height(dimensionResource(R.dimen.margin_small)))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Slider(
                                        value = checkInterval,
                                        onValueChange = { checkInterval = it },
                                        onValueChangeFinished = {
                                            context.save(
                                                PREFERENCE_UPDATES_CHECK_INTERVAL,
                                                checkInterval.roundToInt()
                                            )
                                            onUpdateAutomatedCheck()
                                        },
                                        valueRange = 1f..24f,
                                        steps = 22,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(Modifier.width(dimensionResource(R.dimen.margin_small)))
                                    Text(checkInterval.roundToInt().toString())
                                }
                            }
                        }
                    )
                }
                item {
                    ListItem(
                        modifier = Modifier.clickable { showRestrictionsDialog = true },
                        headlineContent = {
                            Text(stringResource(R.string.pref_updates_restrictions_title))
                        },
                        supportingContent = {
                            Text(stringResource(R.string.pref_updates_restrictions_desc))
                        }
                    )
                }
            }
            item { HorizontalDivider() }
            item {
                ListItem(headlineContent = {
                    Text(stringResource(R.string.pref_updates_app_source))
                })
            }
            item {
                ListItem(
                    modifier = Modifier.clickable {
                        filterFDroid = !filterFDroid
                        context.save(PREFERENCE_FILTER_FDROID, filterFDroid)
                        onCheckUpdatesNow()
                    },
                    headlineContent = { Text(stringResource(R.string.pref_filter_fdroid_title)) },
                    supportingContent = {
                        Text(stringResource(R.string.pref_filter_fdroid_summary))
                    },
                    trailingContent = {
                        Switch(
                            checked = filterFDroid,
                            onCheckedChange = { checked ->
                                filterFDroid = checked
                                context.save(PREFERENCE_FILTER_FDROID, checked)
                                onCheckUpdatesNow()
                            }
                        )
                    }
                )
            }
            item {
                ListItem(
                    modifier = Modifier.clickable {
                        filterAuroraOnly = !filterAuroraOnly
                        context.save(PREFERENCE_FILTER_AURORA_ONLY, filterAuroraOnly)
                        if (filterAuroraOnly) filterFDroid = false
                        onCheckUpdatesNow()
                    },
                    headlineContent = { Text(stringResource(R.string.pref_aurora_only)) },
                    supportingContent = { Text(stringResource(R.string.pref_aurora_only_desc)) },
                    trailingContent = {
                        Switch(
                            checked = filterAuroraOnly,
                            onCheckedChange = { checked ->
                                filterAuroraOnly = checked
                                context.save(PREFERENCE_FILTER_AURORA_ONLY, checked)
                                if (checked) filterFDroid = false
                                onCheckUpdatesNow()
                            }
                        )
                    }
                )
            }
            item { HorizontalDivider() }
            item {
                ListItem(headlineContent = { Text(stringResource(R.string.pref_common_advanced)) })
            }
            item {
                ListItem(
                    modifier = Modifier.clickable {
                        updatesExtended = !updatesExtended
                        context.save(PREFERENCE_UPDATES_EXTENDED, updatesExtended)
                        onCheckUpdatesNow()
                    },
                    headlineContent = { Text(stringResource(R.string.pref_updates_incompatible)) },
                    supportingContent = {
                        Text(stringResource(R.string.pref_updates_incompatible_desc))
                    },
                    trailingContent = {
                        Switch(
                            checked = updatesExtended,
                            onCheckedChange = { checked ->
                                updatesExtended = checked
                                context.save(PREFERENCE_UPDATES_EXTENDED, checked)
                                onCheckUpdatesNow()
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun UpdatesRestrictionsDialog(onUpdateAutomatedCheck: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var metered by remember {
        mutableStateOf(
            Preferences.getBoolean(context, PREFERENCES_UPDATES_RESTRICTIONS_METERED, true)
        )
    }
    var idle by remember {
        mutableStateOf(Preferences.getBoolean(context, PREFERENCES_UPDATES_RESTRICTIONS_IDLE, true))
    }
    var battery by remember {
        mutableStateOf(
            Preferences.getBoolean(context, PREFERENCES_UPDATES_RESTRICTIONS_BATTERY, true)
        )
    }

    DisposableEffect(Unit) {
        onDispose { onUpdateAutomatedCheck() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pref_updates_restrictions_title)) },
        text = {
            Column {
                Text(stringResource(R.string.pref_updates_restrictions_desc))
                Spacer(Modifier.height(dimensionResource(R.dimen.margin_small)))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            metered = !metered
                            Preferences.putBoolean(
                                context,
                                PREFERENCES_UPDATES_RESTRICTIONS_METERED,
                                metered
                            )
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = metered,
                        onCheckedChange = { checked ->
                            metered = checked
                            Preferences.putBoolean(
                                context,
                                PREFERENCES_UPDATES_RESTRICTIONS_METERED,
                                checked
                            )
                        }
                    )
                    Text(stringResource(R.string.pref_updates_restrictions_metered))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            idle = !idle
                            Preferences.putBoolean(
                                context,
                                PREFERENCES_UPDATES_RESTRICTIONS_IDLE,
                                idle
                            )
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = idle,
                        onCheckedChange = { checked ->
                            idle = checked
                            Preferences.putBoolean(
                                context,
                                PREFERENCES_UPDATES_RESTRICTIONS_IDLE,
                                checked
                            )
                        }
                    )
                    Text(stringResource(R.string.pref_updates_restrictions_idle))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            battery = !battery
                            Preferences.putBoolean(
                                context,
                                PREFERENCES_UPDATES_RESTRICTIONS_BATTERY,
                                battery
                            )
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = battery,
                        onCheckedChange = { checked ->
                            battery = checked
                            Preferences.putBoolean(
                                context,
                                PREFERENCES_UPDATES_RESTRICTIONS_BATTERY,
                                checked
                            )
                        }
                    )
                    Text(stringResource(R.string.pref_updates_restrictions_battery))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.ok))
            }
        }
    )
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview
@Composable
private fun UpdatesPreferenceScreenPreview() {
    ScreenContent()
}
