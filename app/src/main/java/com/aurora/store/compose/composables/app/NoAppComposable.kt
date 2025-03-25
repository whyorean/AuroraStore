/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composables.app

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
 * Composable to show error message when no apps are available for a request
 * @param modifier Modifier for the composable
 * @param icon Drawable for error
 * @param message Message for error
 * @param actionMessage Message to show on action button; defaults to null with button not visible
 * @param onAction Callback when action button is clicked
 * @see NoAppAltComposable
 */
@Composable
fun NoAppComposable(
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int,
    @StringRes message: Int,
    @StringRes actionMessage: Int? = null,
    onAction: () -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(
            dimensionResource(R.dimen.margin_small),
            Alignment.CenterVertically
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            modifier = Modifier.requiredSize(dimensionResource(R.dimen.icon_size))
        )
        Text(text = stringResource(message))

        if (actionMessage != null) {
            Button(onClick = onAction) {
                Text(
                    text = stringResource(actionMessage),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NoAppComposablePreview() {
    NoAppComposable(
        icon = R.drawable.ic_updates,
        message = R.string.details_no_updates,
        actionMessage = R.string.check_updates
    )
}
