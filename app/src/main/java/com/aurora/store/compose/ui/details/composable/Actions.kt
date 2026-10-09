/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.aurora.store.R
import com.aurora.store.compose.composable.ExpressiveMenuItem
import com.aurora.store.compose.preview.ThemePreviewProvider

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
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun Actions(
    primaryActionDisplayName: String,
    secondaryActionDisplayName: String,
    isPrimaryActionEnabled: Boolean = true,
    isSecondaryActionEnabled: Boolean = true,
    onPrimaryAction: () -> Unit = {},
    onSecondaryAction: () -> Unit = {}
) {
    val interactionSources = remember { List(2) { MutableInteractionSource() } }
    val leadingShapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
    val trailingShapes = ButtonGroupDefaults.connectedTrailingButtonShapes()

    ButtonGroup(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = dimensionResource(R.dimen.width_button) * 2)
            .padding(horizontal = dimensionResource(R.dimen.spacing_large)),
        overflowIndicator = { menuState ->
            ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
        },
        horizontalArrangement = Arrangement.spacedBy(
            ButtonGroupDefaults.ConnectedSpaceBetween
        )
    ) {
        val secondaryModifier = Modifier.weight(0.95f).animateWidth(interactionSources[0])
        val primaryModifier = Modifier.weight(1.05f).animateWidth(interactionSources[1])

        customItem(
            buttonGroupContent = {
                FilledTonalButton(
                    modifier = secondaryModifier,
                    onClick = onSecondaryAction,
                    enabled = isSecondaryActionEnabled,
                    interactionSource = interactionSources[0],
                    shapes = ButtonDefaults.shapes(
                        shape = leadingShapes.shape,
                        pressedShape = leadingShapes.pressedShape
                    )
                ) {
                    Text(
                        text = secondaryActionDisplayName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            menuContent = { menuState ->
                ExpressiveMenuItem(
                    index = 0,
                    count = 2,
                    text = { Text(text = secondaryActionDisplayName) },
                    enabled = isSecondaryActionEnabled,
                    onClick = {
                        menuState.dismiss()
                        onSecondaryAction()
                    }
                )
            }
        )

        customItem(
            buttonGroupContent = {
                Button(
                    modifier = primaryModifier,
                    onClick = onPrimaryAction,
                    enabled = isPrimaryActionEnabled,
                    interactionSource = interactionSources[1],
                    shapes = ButtonDefaults.shapes(
                        shape = trailingShapes.shape,
                        pressedShape = trailingShapes.pressedShape
                    )
                ) {
                    Text(
                        text = primaryActionDisplayName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            menuContent = { menuState ->
                ExpressiveMenuItem(
                    index = 1,
                    count = 2,
                    text = { Text(text = primaryActionDisplayName) },
                    enabled = isPrimaryActionEnabled,
                    onClick = {
                        menuState.dismiss()
                        onPrimaryAction()
                    }
                )
            }
        )
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun ActionsPreview() {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
    ) {
        Actions(
            primaryActionDisplayName = stringResource(R.string.action_install),
            secondaryActionDisplayName = stringResource(R.string.title_manual_download)
        )
    }
}
