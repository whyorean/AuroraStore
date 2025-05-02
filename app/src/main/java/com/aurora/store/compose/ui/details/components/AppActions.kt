/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.window.core.layout.WindowWidthSizeClass
import com.aurora.store.R

/**
 * Composable to display primary and secondary actions available for the app, supposed to be used
 * as a part of the Column with proper vertical arrangement spacing in the AppDetailsScreen.
 * @param primaryActionDisplayName Name of the primary action
 * @param secondaryActionDisplayName Name of the secondary action
 * @param isPrimaryActionEnabled Whether the primary action is enabled
 * @param isSecondaryActionEnabled Whether the secondary action is enabled
 * @param onPrimaryAction Callback when the primary action is clicked
 * @param onSecondaryAction Callback when the secondary action is clicked
 * @param windowAdaptiveInfo Adaptive window information
 */
@Composable
fun AppActions(
    primaryActionDisplayName: String,
    secondaryActionDisplayName: String,
    isPrimaryActionEnabled: Boolean = true,
    isSecondaryActionEnabled: Boolean = true,
    onPrimaryAction: () -> Unit = {},
    onSecondaryAction: () -> Unit = {},
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo()
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_medium))
    ) {
        val buttonWidthModifier = when (windowAdaptiveInfo.windowSizeClass.windowWidthSizeClass) {
            WindowWidthSizeClass.COMPACT -> Modifier.weight(1F)
            else -> Modifier.widthIn(min = dimensionResource(R.dimen.width_button))
        }

        FilledTonalButton(
            modifier = buttonWidthModifier,
            onClick = onSecondaryAction,
            enabled = isSecondaryActionEnabled
        ) {
            Text(
                text = secondaryActionDisplayName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Button(
            modifier = buttonWidthModifier,
            onClick = onPrimaryAction,
            enabled = isPrimaryActionEnabled
        ) {
            Text(
                text = primaryActionDisplayName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppActionsPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_medium))) {
        AppActions(
            primaryActionDisplayName = stringResource(R.string.action_install),
            secondaryActionDisplayName = stringResource(R.string.title_manual_download)
        )
    }
}
