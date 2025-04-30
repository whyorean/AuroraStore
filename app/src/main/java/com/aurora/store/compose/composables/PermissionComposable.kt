/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.aurora.store.R
import com.aurora.store.data.model.Permission
import com.aurora.store.data.model.PermissionType

/**
 * Composable to display permission details in a list
 * @param modifier The modifier to be applied to the composable
 * @param permission [Permission] to display
 * @param isGranted If the permission has been granted
 * @param onAction Callback when the user clicks the action button
 */
@Composable
fun PermissionComposable(
    modifier: Modifier = Modifier,
    permission: Permission,
    isGranted: Boolean = false,
    onAction: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(dimensionResource(R.dimen.padding_small)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1F)) {
            Text(
                text = permission.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = permission.subtitle,
                style = MaterialTheme.typography.bodySmall
            )
        }
        TextButton(onClick = onAction, enabled = !isGranted) {
            Text(
                text = if (isGranted) {
                    stringResource(R.string.action_granted)
                } else {
                    stringResource(R.string.action_grant)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PermissionComposablePreview() {
    PermissionComposable(
        permission = Permission(
            PermissionType.STORAGE_MANAGER,
            LocalContext.current.getString(R.string.onboarding_permission_esm),
            LocalContext.current.getString(R.string.onboarding_permission_esa_desc),
            false
        )
    )
}
