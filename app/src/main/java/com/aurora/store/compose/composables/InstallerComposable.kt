/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.aurora.store.R
import com.aurora.store.data.installer.SessionInstaller
import com.aurora.store.data.model.InstallerInfo

/**
 * Composable to display installer details in a list
 * @param modifier The modifier to be applied to the composable
 * @param installerInfo A [InstallerInfo] object to display details
 * @param isSelected Whether this installer is selected
 * @param onClick Callback when this composable is clicked
 */
@Composable
fun InstallerComposable(
    modifier: Modifier = Modifier,
    installerInfo: InstallerInfo,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(dimensionResource(R.dimen.padding_small)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1F)) {
            Text(
                text = stringResource(installerInfo.title),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(installerInfo.subtitle),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(installerInfo.description),
                style = MaterialTheme.typography.bodySmall
            )
        }
        RadioButton(selected = isSelected, onClick = onClick)
    }
}

@Preview(showBackground = true)
@Composable
private fun InstallerComposablePreview() {
    InstallerComposable(installerInfo = SessionInstaller.installerInfo, isSelected = true)
}
