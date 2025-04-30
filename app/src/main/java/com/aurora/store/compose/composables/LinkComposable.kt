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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.aurora.store.R
import com.aurora.store.data.model.Link

/**
 * Composable to show link details in a list
 * @param modifier The modifier to be applied to the composable
 * @param link [Link] to show details
 * @param onClick Callback when the composable is clicked
 */
@Composable
fun LinkComposable(modifier: Modifier = Modifier, link: Link, onClick: () -> Unit = {}) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(dimensionResource(R.dimen.padding_small)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_medium))
    ) {
        Icon(
            painter = painterResource(link.icon),
            contentDescription = null
        )
        VerticalDivider(
            modifier = Modifier
                .padding(horizontal = dimensionResource(R.dimen.margin_large))
                .width(dimensionResource(R.dimen.padding_xxsmall))
                .height(dimensionResource(R.dimen.height_header))
        )
        Column {
            Text(
                text = link.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = link.subtitle,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LinkComposablePreview() {
    LinkComposable(
        link = Link(
            id = 0,
            title = LocalContext.current.getString(R.string.title_about),
            subtitle = LocalContext.current.getString(R.string.about_aurora_store_subtitle),
            url = "https://auroraoss.com/",
            icon = R.drawable.ic_menu_about
        )
    )
}
