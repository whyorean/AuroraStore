/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.aurora.store.R
import com.aurora.store.compose.preview.ThemePreviewProvider

/**
 * Single shared layout for every list-row in the app. A leading slot (icon, image…), a
 * stacked headline + supporting + tertiary text column, and a trailing slot (checkbox,
 * button, chip…). [headlineStyle] toggles between data-row (bodyMedium) and settings-row
 * (bodyLarge) typography while keeping every other dimension consistent.
 */
@Composable
fun AuroraListItem(
    modifier: Modifier = Modifier,
    headline: String,
    supporting: String? = null,
    tertiary: String? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    headlineStyle: TextStyle = MaterialTheme.typography.bodyLarge
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(enabled = enabled, onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(
                horizontal = dimensionResource(R.dimen.spacing_medium),
                vertical = dimensionResource(R.dimen.spacing_xsmall)
            ),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) leading()
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = headline,
                style = headlineStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!supporting.isNullOrBlank()) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!tertiary.isNullOrBlank()) {
                Text(
                    text = tertiary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) trailing()
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun AuroraListItemPreview() {
    AuroraListItem(
        headline = "Example app",
        supporting = "com.example.app",
        tertiary = "v1.0.0 (100)"
    )
}
