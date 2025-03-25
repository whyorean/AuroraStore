/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composables

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.compose.composables.preview.AppPreviewProvider

/**
 * Composable to show some information
 * @param title Title of the information
 * @param description Information to show
 * @param icon Optional icon representing the information
 * @param onClick Callback when this composable is clicked
 */
@Composable
fun InfoComposable(
    title: AnnotatedString,
    description: AnnotatedString? = null,
    @DrawableRes icon: Int? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { if (onClick != null) onClick() }, enabled = onClick != null)
            .padding(
                horizontal = dimensionResource(R.dimen.padding_small),
                vertical = dimensionResource(R.dimen.padding_xxsmall)
            ),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_normal)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) Icon(painter = painterResource(icon), contentDescription = null)
        Column(modifier = Modifier.weight(1F)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun InfoComposablePreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    InfoComposable(
        title = AnnotatedString(text = stringResource(R.string.details_dev_website)),
        description = AnnotatedString.fromHtml(htmlString = app.developerWebsite),
        icon = R.drawable.ic_network
    )
}
