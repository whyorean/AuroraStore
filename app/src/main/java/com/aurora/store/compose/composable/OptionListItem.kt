/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.aurora.store.compose.navigation.Screen
import com.aurora.store.data.model.ComposeOption
import com.aurora.store.data.model.Option

/**
 * Composable to display UI navigation options
 * @param modifier Modifier for the composable
 * @param option Option to display
 * @param onClick Callback when this composable is clicked
 */
@Composable
fun OptionListItem(modifier: Modifier = Modifier, option: Option, onClick: () -> Unit = {}) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(dimensionResource(R.dimen.padding_normal)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            dimensionResource(R.dimen.margin_small),
            Alignment.Start
        )
    ) {
        Icon(
            painter = painterResource(id = option.icon),
            contentDescription = stringResource(id = option.title),
            modifier = Modifier
                .padding(horizontal = dimensionResource(R.dimen.padding_medium))
                .requiredSize(dimensionResource(R.dimen.icon_size_default))
        )
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = option.title),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OptionListItemPreview() {
    OptionListItem(
        option = ComposeOption(
            title = R.string.title_apps_games,
            icon = R.drawable.ic_apps,
            screen = Screen.Installed
        )
    )
}
