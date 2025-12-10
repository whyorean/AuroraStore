/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.dispenser

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.aurora.store.R

/**
 * Dialog for removing a token dispenser
 * @param onRemove Callback on removing a dispenser
 * @param onDismiss Callback on dismiss
 */
@Composable
fun RemoveDispenserDialog(url: String, onRemove: () -> Unit = {}, onDismiss: () -> Unit = {}) {
    AlertDialog(
        title = { Text(text = stringResource(R.string.remove_dispenser_title)) },
        text = {
            Text(text = stringResource(R.string.remove_dispenser_summary, url))
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onRemove) {
                Text(text = stringResource(R.string.remove))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }
        }
    )
}

@Preview
@Composable
private fun RemoveDispenserDialogPreview() {
    RemoveDispenserDialog(url = "https://auroraoss.com/api/auth/")
}
