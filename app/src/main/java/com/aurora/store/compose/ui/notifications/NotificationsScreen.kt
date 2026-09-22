/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.notifications

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.compose.composable.AuroraListItem
import com.aurora.store.compose.composable.Placeholder
import com.aurora.store.compose.composable.TopAppBar
import com.aurora.store.compose.composable.app.AnimatedAppIcon
import com.aurora.store.compose.navigation.Destination
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.ThemePreviewProvider
import com.aurora.store.data.model.DownloadStatus
import com.aurora.store.data.model.NotificationEntry
import com.aurora.store.data.room.download.Download
import com.aurora.store.viewmodel.notifications.NotificationsViewModel

@Composable
fun NotificationsScreen(
    onNavigateTo: (Destination) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val installing by viewModel.installing.collectAsStateWithLifecycle()

    ScreenContent(
        entries = entries,
        installing = installing,
        onInstall = { viewModel.install(it.download.packageName) },
        onDismiss = { viewModel.dismiss(it) },
        onClick = { entry ->
            viewModel.markRead(entry)
            val packageName = when (entry) {
                is NotificationEntry.PendingInstall -> entry.download.packageName
                is NotificationEntry.Remote -> entry.notification.packageName
            }
            packageName?.let { onNavigateTo(Destination.AppDetails(it)) }
        }
    )
}

@Composable
private fun ScreenContent(
    entries: List<NotificationEntry> = emptyList(),
    installing: Set<String> = emptySet(),
    onInstall: (NotificationEntry.PendingInstall) -> Unit = {},
    onDismiss: (NotificationEntry) -> Unit = {},
    onClick: (NotificationEntry) -> Unit = {}
) {
    Scaffold(
        topBar = { TopAppBar(title = stringResource(R.string.title_notifications)) }
    ) { paddingValues ->
        if (entries.isEmpty()) {
            Placeholder(
                modifier = Modifier.padding(paddingValues),
                painter = painterResource(R.drawable.ic_notifications),
                message = stringResource(R.string.notification_none)
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentPadding = PaddingValues(
                vertical = dimensionResource(R.dimen.spacing_small)
            )
        ) {
            items(items = entries, key = { it.id }) { entry ->
                when (entry) {
                    is NotificationEntry.PendingInstall -> PendingInstallItem(
                        modifier = Modifier.animateItem(),
                        download = entry.download,
                        isInstalling = entry.download.packageName in installing,
                        onInstall = { onInstall(entry) },
                        onDismiss = { onDismiss(entry) },
                        onClick = { onClick(entry) }
                    )

                    is NotificationEntry.Remote -> AuroraListItem(
                        modifier = Modifier.animateItem(),
                        headline = entry.notification.title,
                        supporting = entry.notification.body,
                        tertiary = relativeTime(entry.timestamp),
                        onClick = { onClick(entry) },
                        trailing = {
                            TextButton(onClick = { onDismiss(entry) }) {
                                Text(stringResource(R.string.action_clear))
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingInstallItem(
    modifier: Modifier = Modifier,
    download: Download,
    isInstalling: Boolean,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
    onClick: () -> Unit
) {
    AuroraListItem(
        modifier = modifier,
        headline = download.displayName,
        supporting = if (isInstalling) {
            stringResource(DownloadStatus.INSTALLING.localized)
        } else {
            stringResource(download.status.localized)
        },
        tertiary = relativeTime(download.downloadedAt),
        headlineStyle = MaterialTheme.typography.bodyMedium,
        onClick = onClick,
        leading = {
            AnimatedAppIcon(
                modifier = Modifier.requiredSize(dimensionResource(R.dimen.icon_size_medium)),
                iconUrl = download.iconURL,
                inProgress = isInstalling
            )
        },
        trailing = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(
                    dimensionResource(R.dimen.spacing_xsmall)
                )
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_clear))
                }
                if (!isInstalling) {
                    FilledTonalButton(onClick = onInstall) {
                        Text(stringResource(R.string.action_install))
                    }
                }
            }
        }
    )
}

private fun relativeTime(timestamp: Long): String? {
    if (timestamp <= 0) return null
    val now = System.currentTimeMillis()
    if (now - timestamp < DateUtils.MINUTE_IN_MILLIS) return null
    return DateUtils.getRelativeTimeSpanString(timestamp, now, DateUtils.MINUTE_IN_MILLIS)
        .toString()
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview
@Composable
private fun NotificationsScreenPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    ScreenContent(
        entries = listOf(NotificationEntry.PendingInstall(Download.fromApp(app)))
    )
}
