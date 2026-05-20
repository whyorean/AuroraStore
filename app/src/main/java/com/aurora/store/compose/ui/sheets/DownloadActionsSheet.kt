/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.sheets

import android.text.format.Formatter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.aurora.store.R
import com.aurora.store.compose.composable.app.AnimatedAppIcon
import com.aurora.store.data.room.download.Download
import com.aurora.store.util.PackageUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadActionsSheet(
    download: Download,
    onDismiss: () -> Unit,
    onShowDetails: () -> Unit,
    onCancel: () -> Unit,
    onInstall: () -> Unit,
    onExport: () -> Unit,
    onClear: () -> Unit
) {
    val context = LocalContext.current
    val canInstall = download.canInstall(context)
    val canExport = canInstall || PackageUtil.isInstalled(context, download.packageName)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            DownloadHeader(
                download = download,
                onShowDetails = {
                    onShowDetails()
                    onDismiss()
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = dimensionResource(R.dimen.spacing_xsmall))
            )

            if (download.isRunning) {
                Item(
                    label = stringResource(R.string.action_cancel),
                    enabled = download.isRunning,
                    onClick = {
                        onCancel()
                        onDismiss()
                    }
                )
            } else {
                Item(
                    label = stringResource(R.string.action_clear),
                    enabled = !download.isRunning,
                    onClick = {
                        onClear()
                        onDismiss()
                    }
                )
            }

            if (!PackageUtil.isInstalled(context, download.packageName)) {
                Item(
                    label = stringResource(R.string.action_install),
                    enabled = canInstall,
                    onClick = {
                        onInstall()
                        onDismiss()
                    }
                )
            }

            Item(
                label = stringResource(R.string.action_export),
                enabled = canExport,
                onClick = {
                    onExport()
                    onDismiss()
                }
            )

            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun DownloadHeader(download: Download, onShowDetails: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.spacing_medium),
                vertical = dimensionResource(R.dimen.spacing_xsmall)
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
    ) {
        AnimatedAppIcon(
            modifier = Modifier.requiredSize(dimensionResource(R.dimen.icon_size_medium)),
            iconUrl = download.iconURL,
            inProgress = download.isRunning,
            progress = download.progress.toFloat()
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = download.displayName,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = download.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = Formatter.formatShortFileSize(LocalContext.current, download.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        FilledTonalButton(onClick = onShowDetails) {
            Text(stringResource(R.string.updates_app_details))
        }
    }
}

@Composable
private fun Item(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        color = if (enabled) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.spacing_xsmall))
            .clip(RoundedCornerShape(dimensionResource(R.dimen.radius_medium)))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(
                horizontal = dimensionResource(R.dimen.spacing_medium),
                vertical = dimensionResource(R.dimen.spacing_small)
            )
    )
}
