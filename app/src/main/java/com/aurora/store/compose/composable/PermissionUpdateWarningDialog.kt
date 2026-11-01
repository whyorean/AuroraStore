/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable

import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.aurora.store.R
import com.aurora.store.compose.preview.ThemePreviewProvider

@Composable
fun PermissionUpdateWarningDialog(
    permissions: List<String>,
    onConfirm: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    val packageManager = LocalContext.current.packageManager
    val labels = remember(permissions) {
        permissions.map { permission ->
            try {
                packageManager.getPermissionInfo(permission, 0)
                    .loadLabel(packageManager)
                    .toString()
            } catch (_: PackageManager.NameNotFoundException) {
                permission
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.permission_warning_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.permission_warning_desc))
                Spacer(Modifier.height(dimensionResource(R.dimen.spacing_small)))
                labels.forEach { label ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(
                            dimensionResource(R.dimen.spacing_small)
                        )
                    ) {
                        Text("•")
                        Text(modifier = Modifier.weight(1f), text = label)
                    }
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
private fun PermissionUpdateWarningDialogPreview() {
    PermissionUpdateWarningDialog(
        permissions = listOf(
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.READ_CONTACTS"
        )
    )
}
