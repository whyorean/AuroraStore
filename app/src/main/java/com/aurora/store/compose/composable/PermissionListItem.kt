/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.aurora.store.R
import com.aurora.store.compose.preview.ThemePreviewProvider
import com.aurora.store.data.model.Permission
import com.aurora.store.data.model.PermissionType

@Composable
fun PermissionListItem(
    modifier: Modifier = Modifier,
    permission: Permission,
    onAction: () -> Unit = {}
) {
    AuroraListItem(
        modifier = modifier,
        headline = permission.title,
        supporting = permission.subtitle,
        trailing = {
            TextButton(onClick = onAction, enabled = !permission.isGranted) {
                Text(
                    text = if (permission.isGranted) {
                        stringResource(R.string.action_granted)
                    } else {
                        stringResource(R.string.action_grant)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    )
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun PermissionListItemPreview() {
    PermissionListItem(
        permission = Permission(
            PermissionType.STORAGE_MANAGER,
            stringResource(R.string.onboarding_permission_esm),
            stringResource(R.string.onboarding_permission_esa_desc),
            false
        )
    )
}
