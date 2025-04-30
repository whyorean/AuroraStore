/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.aurora.store.R

/**
 * Composable to display dispenser URL in a list
 * @param modifier The modifier to be applied to the composable
 * @param url URL of the dispenser
 * @param onClick Callback when this URL is clicked
 * @param onClear Callback when the clear button is clicked
 */
@Composable
fun DispenserComposable(
    modifier: Modifier = Modifier,
    url: String,
    onClick: () -> Unit = {},
    onClear: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(dimensionResource(R.dimen.padding_small)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1F),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_small))
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_server),
                contentDescription = null
            )
            Text(
                text = url,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        TextButton(onClick = onClear) {
            Text(
                text = stringResource(R.string.remove),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DispenserComposablePreview() {
    DispenserComposable(url = "https://auroraoss.com/api/auth")
}
