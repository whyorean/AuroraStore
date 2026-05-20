/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.aurora.store.R

@Composable
fun DispenserListItem(
    modifier: Modifier = Modifier,
    url: String,
    onClick: () -> Unit = {},
    onClear: () -> Unit = {}
) {
    AuroraListItem(
        modifier = modifier,
        headline = url,
        onClick = onClick,
        leading = {
            Icon(painter = painterResource(R.drawable.ic_server), contentDescription = null)
        },
        trailing = {
            TextButton(onClick = onClear) {
                Text(
                    text = stringResource(R.string.remove),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun DispenserListItemPreview() {
    DispenserListItem(url = "https://auroraoss.com/api/auth")
}
