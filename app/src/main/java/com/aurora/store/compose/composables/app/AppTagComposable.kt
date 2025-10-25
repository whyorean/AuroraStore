/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composables.app

import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.aurora.store.R

/**
 * Composable to show a tag related to an app
 * @param modifier The modifier to be applied to the composable
 * @param label Label of the tag
 * @param painter Painter to draw the icon
 * @param onClick Callback when this composable is clicked
 */
@Composable
fun AppTagComposable(
    modifier: Modifier = Modifier,
    label: String,
    painter: Painter,
    onClick: () -> Unit = {}
) {
    FilterChip(
        modifier = modifier,
        onClick = onClick,
        label = { Text(text = label, style = MaterialTheme.typography.bodySmall) },
        leadingIcon = { Icon(painter = painter, contentDescription = label) },
        selected = true
    )
}

@Preview(showBackground = true)
@Composable
private fun AppTagComposablePreview() {
    AppTagComposable(
        label = stringResource(R.string.details_free),
        painter = painterResource(R.drawable.ic_paid)
    )
}
