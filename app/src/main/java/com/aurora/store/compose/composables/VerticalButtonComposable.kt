/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.aurora.store.R

/**
 * Composable with an icon and text placed vertically to function as a button
 * @param modifier Modifier
 * @param painter Icon for the button
 * @param text Text describing the button's action
 * @param onClick Callback when this composable is clicked
 */
@Composable
fun VerticalButtonComposable(
    modifier: Modifier = Modifier,
    painter: Painter,
    text: String,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .padding(dimensionResource(R.dimen.padding_xsmall))
            .clip(RoundedCornerShape(dimensionResource(R.dimen.icon_size_default)))
            .clickable(onClick = { if (onClick != null) onClick() }, enabled = onClick != null)
            .padding(dimensionResource(R.dimen.padding_medium)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painter,
            contentDescription = text
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VerticalButtonComposablePreview() {
    VerticalButtonComposable(
        painter = painterResource(R.drawable.ic_file_copy),
        text = stringResource(R.string.action_export)
    )
}
