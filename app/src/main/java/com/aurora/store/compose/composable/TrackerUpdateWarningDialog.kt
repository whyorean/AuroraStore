/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.aurora.store.R
import com.aurora.store.compose.preview.ThemePreviewProvider
import com.aurora.store.data.model.ExodusTracker

@Composable
fun TrackerUpdateWarningDialog(
    trackers: List<ExodusTracker>,
    onConfirm: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tracker_warning_title)) },
        text = {
            Column {
                Text(stringResource(R.string.tracker_warning_desc))
                Spacer(Modifier.height(dimensionResource(R.dimen.spacing_small)))
                trackers.forEach { tracker ->
                    Text("• ${tracker.name}")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.action_update_anyway))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview
@Composable
private fun TrackerUpdateWarningDialogPreview() {
    TrackerUpdateWarningDialog(
        trackers = listOf(
            ExodusTracker(name = "Google AdMob"),
            ExodusTracker(name = "Google Firebase Analytics")
        )
    )
}
