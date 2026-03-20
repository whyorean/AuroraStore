/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurora.store.R
import com.aurora.store.compose.composable.PermissionList
import com.aurora.store.compose.preview.PreviewTemplate
import com.aurora.store.data.model.Permission
import com.aurora.store.data.model.PermissionType
import com.aurora.store.viewmodel.commons.PermissionRationaleViewModel
import kotlin.random.Random

@Composable
fun PermissionsPage(viewModel: PermissionRationaleViewModel = hiltViewModel()) {
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()

    PageContent(
        permissions = permissions,
        onPermissionCallback = { viewModel.refreshPermissionsList() }
    )
}

@Composable
private fun PageContent(
    permissions: List<Permission> = emptyList(),
    onPermissionCallback: (type: PermissionType) -> Unit = {}
) {
    PermissionList(
        modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.padding_medium)),
        permissions = permissions,
        onPermissionCallback = onPermissionCallback,
        header = {
            Surface(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(dimensionResource(R.dimen.padding_medium))
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_title_permissions),
                        style = MaterialTheme.typography.headlineLargeEmphasized,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.onboarding_permission_select),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun PermissionsPagePreview() {
    val permissions = PermissionType.entries.map { type ->
        Permission(
            type = type,
            title = LoremIpsum(3).values.first(),
            subtitle = LoremIpsum(7).values.first(),
            optional = Random.nextBoolean(),
            isGranted = Random.nextBoolean()
        )
    }
    PreviewTemplate {
        PageContent(
            permissions = permissions
        )
    }
}
