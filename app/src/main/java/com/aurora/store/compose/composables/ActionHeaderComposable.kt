/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composables

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.aurora.store.R

/**
 * Composable to display sticky header in list
 * @param title Title to display
 * @param actionTitle Title for the action button
 * @param onAction Callback when action button is clicked
 * @see TextDividerComposable
 * @see HeaderComposable
 */
@Composable
fun ActionHeaderComposable(
    title: String,
    actionTitle: String,
    onAction: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.padding_medium),
                vertical = dimensionResource(R.dimen.padding_xsmall)
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Button(onClick = onAction) {
            Text(
                text = actionTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ActionHeaderComposablePreview() {
    ActionHeaderComposable(
        title = stringResource(R.string.updates_available),
        actionTitle = stringResource(R.string.action_update_all)
    )
}
