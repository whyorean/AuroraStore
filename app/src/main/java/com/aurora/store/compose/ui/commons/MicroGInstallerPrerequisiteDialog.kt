/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-FileCopyrightText: 2026 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.commons

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.aurora.Constants.PACKAGE_NAME_GMS
import com.aurora.extensions.TAG
import com.aurora.store.R
import com.aurora.store.compose.preview.ThemePreviewProvider

private const val MICROG_SETTINGS_ACTIVITY = "org.microg.gms.ui.SettingsActivity"

/**
 * Dialog informing users about the prerequisite for using the microG installer
 * @param onConfirm Callback on confirmation
 * @param onDismiss Callback on dismissal
 */
@Composable
fun MicroGInstallerPrerequisiteDialog(onConfirm: () -> Unit = {}, onDismiss: () -> Unit = {}) {
    val context = LocalContext.current

    AlertDialog(
        title = { Text(text = stringResource(R.string.microg_installer_prerequisite_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(
                    dimensionResource(R.dimen.spacing_small)
                )
            ) {
                Text(text = stringResource(R.string.microg_installer_prerequisite_desc))
                OutlinedButton(
                    onClick = {
                        try {
                            context.startActivity(
                                Intent().apply {
                                    component = ComponentName(
                                        PACKAGE_NAME_GMS,
                                        MICROG_SETTINGS_ACTIVITY
                                    )
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            )
                        } catch (_: ActivityNotFoundException) {
                            Log.i(TAG, "Unable to launch microG settings")
                        }
                    }
                ) {
                    Text(text = stringResource(R.string.microg_installer_open_settings))
                }
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.action_ok))
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
private fun MicroGInstallerPrerequisiteDialogPreview() {
    MicroGInstallerPrerequisiteDialog()
}
