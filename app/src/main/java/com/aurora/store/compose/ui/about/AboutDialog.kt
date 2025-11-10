/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.about

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.aurora.store.R
import com.aurora.store.compose.preview.PreviewTemplate

/**
 * Dialog for displaying information about Aurora Store
 * @param onDismiss Callback on dismiss
 */
@Composable
fun AboutDialog(onDismiss: () -> Unit = {}) {
    AlertDialog(
        title = { Text(text = stringResource(R.string.about_aurora_store_title)) },
        text = {
            Text(text = stringResource(R.string.about_aurora_store_summary))
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.ok))
            }
        }
    )
}

@Preview
@Composable
private fun AboutDialogPreview() {
    PreviewTemplate {
        AboutDialog()
    }
}
