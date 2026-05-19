/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable

import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.aurora.store.compose.preview.ThemePreviewProvider
import com.aurora.store.data.installer.SessionInstaller
import com.aurora.store.data.model.InstallerInfo

@Composable
fun InstallerListItem(
    modifier: Modifier = Modifier,
    installerInfo: InstallerInfo,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    AuroraListItem(
        modifier = modifier,
        headline = stringResource(installerInfo.title),
        supporting = stringResource(installerInfo.subtitle),
        tertiary = stringResource(installerInfo.description),
        onClick = onClick,
        trailing = { RadioButton(selected = isSelected, onClick = onClick) }
    )
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun InstallerListItemPreview() {
    InstallerListItem(installerInfo = SessionInstaller.installerInfo, isSelected = true)
}
