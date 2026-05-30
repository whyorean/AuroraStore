/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.preferences

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.aurora.Constants
import com.aurora.extensions.areNotificationsEnabled
import com.aurora.extensions.isOAndAbove
import com.aurora.store.R
import com.aurora.store.compose.composable.TopAppBar
import com.aurora.store.compose.preview.ThemePreviewProvider
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_NOTIFICATION_PROGRESS
import com.aurora.store.util.save

@Composable
fun NotificationPreferenceScreen() {
    ScreenContent()
}

@Composable
private fun ScreenContent() {
    val context = LocalContext.current

    var notificationsEnabled by remember { mutableStateOf(context.areNotificationsEnabled()) }
    var showProgress by remember {
        mutableStateOf(Preferences.getBoolean(context, PREFERENCE_NOTIFICATION_PROGRESS, true))
    }

    // Re-read the system notification state when returning from settings.
    LifecycleResumeEffect(Unit) {
        notificationsEnabled = context.areNotificationsEnabled()
        onPauseOrDispose {}
    }

    // The channels users most likely want to fine-tune (sound, importance, on/off), paired
    // with their display-name strings, deep-linked into the system per-channel settings.
    val channels = listOf(
        Constants.NOTIFICATION_CHANNEL_DOWNLOADS to R.string.notification_channel_downloads,
        Constants.NOTIFICATION_CHANNEL_INSTALL to R.string.notification_channel_install,
        Constants.NOTIFICATION_CHANNEL_UPDATES to R.string.notification_channel_updates,
        Constants.NOTIFICATION_CHANNEL_ALERTS to R.string.notification_channel_alerts,
        Constants.NOTIFICATION_CHANNEL_EXPORT to R.string.notification_channel_export
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.title_notifications)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (!notificationsEnabled) {
                item {
                    ListItem(
                        modifier = Modifier.clickable { openAppNotificationSettings(context) },
                        headlineContent = {
                            Text(stringResource(R.string.pref_notification_disabled))
                        },
                        supportingContent = {
                            Text(stringResource(R.string.pref_notification_disabled_desc))
                        }
                    )
                }
                item { HorizontalDivider() }
            }

            item {
                ListItem(
                    modifier = Modifier.clickable {
                        showProgress = !showProgress
                        context.save(PREFERENCE_NOTIFICATION_PROGRESS, showProgress)
                    },
                    headlineContent = {
                        Text(stringResource(R.string.pref_notification_progress))
                    },
                    supportingContent = {
                        Text(stringResource(R.string.pref_notification_progress_desc))
                    },
                    trailingContent = {
                        Switch(
                            checked = showProgress,
                            onCheckedChange = { checked ->
                                showProgress = checked
                                context.save(PREFERENCE_NOTIFICATION_PROGRESS, checked)
                            }
                        )
                    }
                )
            }

            // Per-channel system settings are only meaningful on Android O+ where channels
            // exist; on older versions the app-level toggle above is the only control.
            if (isOAndAbove) {
                item { HorizontalDivider() }
                item {
                    ListItem(
                        headlineContent = {
                            Text(stringResource(R.string.pref_notification_categories))
                        },
                        supportingContent = {
                            Text(stringResource(R.string.pref_notification_categories_desc))
                        }
                    )
                }
                channels.forEach { (channelId, nameRes) ->
                    item {
                        ListItem(
                            modifier = Modifier.clickable {
                                openChannelSettings(context, channelId)
                            },
                            headlineContent = { Text(stringResource(nameRes)) }
                        )
                    }
                }
            }
        }
    }
}

private fun openAppNotificationSettings(context: android.content.Context) {
    val intent = if (isOAndAbove) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData("package:${context.packageName}".toUri())
    }
    context.startActivity(intent)
}

private fun openChannelSettings(context: android.content.Context, channelId: String) {
    val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
    context.startActivity(intent)
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview
@Composable
private fun NotificationPreferenceScreenPreview() {
    ScreenContent()
}
