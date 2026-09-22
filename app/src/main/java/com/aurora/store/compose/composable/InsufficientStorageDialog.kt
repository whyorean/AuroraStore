/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.aurora.store.R
import com.aurora.store.compose.preview.ThemePreviewProvider
import com.aurora.store.data.model.StorageRequirement

@Composable
fun InsufficientStorageDialog(
    requirement: StorageRequirement,
    onFreeUpSpace: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val required = Formatter.formatShortFileSize(context, requirement.required)
    val available = Formatter.formatShortFileSize(context, requirement.available)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.insufficient_storage_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(
                    dimensionResource(R.dimen.spacing_small)
                )
            ) {
                Text(
                    text = if (requirement.appName != null) {
                        stringResource(
                            R.string.insufficient_storage_desc,
                            requirement.appName,
                            required,
                            available
                        )
                    } else {
                        pluralStringResource(
                            R.plurals.insufficient_storage_desc_multiple,
                            requirement.appCount,
                            requirement.appCount,
                            required,
                            available
                        )
                    }
                )
                Text(
                    text = stringResource(
                        R.string.insufficient_storage_hint,
                        Formatter.formatShortFileSize(context, requirement.shortfall)
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onFreeUpSpace) {
                Text(text = stringResource(R.string.action_free_up_space))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_cancel))
            }
        }
    )
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview
@Composable
private fun InsufficientStorageDialogPreview() {
    InsufficientStorageDialog(
        requirement = StorageRequirement(
            required = 172_000_000,
            available = 86_000_000,
            appName = "Instagram"
        )
    )
}
