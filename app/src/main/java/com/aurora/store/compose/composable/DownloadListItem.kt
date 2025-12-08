/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable

import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.compose.composable.app.AnimatedAppIcon
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.PreviewTemplate
import com.aurora.store.data.model.DownloadStatus
import com.aurora.store.data.room.download.Download
import com.aurora.store.util.CommonUtil.getETAString
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date

/**
 * Composable to display details of a download in a list
 * @param modifier The modifier to be applied to the composable
 * @param download [Download] to display
 * @param onClick Callback when this composable is clicked
 */
@Composable
fun DownloadListItem(
    modifier: Modifier = Modifier,
    download: Download,
    onClick: () -> Unit = {},
    onClear: () -> Unit = {},
    onCancel: () -> Unit = {},
    onExport: () -> Unit = {},
    onInstall: () -> Unit = {},
    isExpanded: Boolean = false
) {
    val progress = "${download.progress}%"
    val speed = "${Formatter.formatShortFileSize(LocalContext.current, download.speed)}/s"
    val eta = getETAString(LocalContext.current, download.timeRemaining)

    val coroutineScope = rememberCoroutineScope()
    var isVisible by remember { mutableStateOf(true) }
    var isExpanded by remember { mutableStateOf(isExpanded) }

    fun requestClear() {
        coroutineScope.launch {
            isVisible = false
            delay(300) // Let the animation play
            onClear()
        }
    }

    @Composable
    fun ExpandedMenu() {
        val context = LocalContext.current
        Box(modifier = modifier) {
            IconButton(onClick = { isExpanded = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_more_vert),
                    contentDescription = stringResource(R.string.menu)
                )
            }
            DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.action_cancel)) },
                    onClick = onCancel,
                    enabled = download.isRunning
                )
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.action_install)) },
                    onClick = onInstall,
                    enabled = download.canInstall(context)
                )
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.action_export)) },
                    onClick = onExport,
                    enabled = download.canInstall(context)
                )
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.action_clear)) },
                    onClick = ::requestClear,
                    enabled = !download.isRunning
                )
            }
        }
    }

    AnimatedVisibility(visible = isVisible, exit = shrinkVertically() + fadeOut()) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(
                    horizontal = dimensionResource(R.dimen.padding_medium),
                    vertical = dimensionResource(R.dimen.padding_small)
                ),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1F)) {
                AnimatedAppIcon(
                    modifier = Modifier.requiredSize(dimensionResource(R.dimen.icon_size_medium)),
                    iconUrl = download.iconURL,
                    inProgress = download.isRunning,
                    progress = download.progress.toFloat()
                )
                Column(
                    modifier = Modifier
                        .padding(horizontal = dimensionResource(R.dimen.margin_small)),
                ) {
                    Text(
                        text = download.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(download.status.localized),
                        style = MaterialTheme.typography.bodySmall
                    )
                    AnimatedContent(targetState = download.status) { status ->
                        Text(
                            style = MaterialTheme.typography.bodySmall,
                            text = if (status in DownloadStatus.running) {
                                "$progress • $speed • $eta"
                            } else {
                                DateUtils.getRelativeTimeSpanString(
                                    download.downloadedAt,
                                    Date().time,
                                    DateUtils.DAY_IN_MILLIS
                                ).toString()
                            }
                        )
                    }
                }
            }
            ExpandedMenu()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DownloadListItemPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    PreviewTemplate {
        DownloadListItem(download = Download.fromApp(app), isExpanded = true)
    }
}
