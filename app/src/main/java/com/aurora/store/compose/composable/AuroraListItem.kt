/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.aurora.store.R
import com.aurora.store.compose.preview.ThemePreviewProvider

/**
 * Shared Material 3 list row used by app, download, account, and settings lists.
 * Keeping the leading, text, and trailing slots here gives these screens one spacing system.
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
    val supportingContent: (@Composable () -> Unit)? =
        if (!supporting.isNullOrBlank() || !tertiary.isNullOrBlank()) {
            {
                Column {
                    if (!supporting.isNullOrBlank()) {
                        Text(
                            text = supporting,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!tertiary.isNullOrBlank()) {
                        Text(
                            text = tertiary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            null
        }
    val itemModifier = modifier.fillMaxWidth()
    val itemColors = ListItemDefaults.colors(containerColor = Color.Transparent)
    val contentPadding = PaddingValues(
        horizontal = dimensionResource(R.dimen.spacing_large),
        vertical = dimensionResource(R.dimen.spacing_small)
    )

    if (onClick != null) {
        ListItem(
            onClick = onClick,
            modifier = itemModifier,
            enabled = enabled,
            shapes = auroraListItemShapes(),
            supportingContent = supportingContent,
            leadingContent = leading,
            trailingContent = trailing,
            colors = itemColors,
            contentPadding = contentPadding
        ) {
            Text(text = headline, style = headlineStyle)
        }
    } else {
        ListItem(
            modifier = itemModifier,
            enabled = enabled,
            shapes = auroraListItemShapes(),
            supportingContent = supportingContent,
            leadingContent = leading,
            trailingContent = trailing,
            colors = itemColors,
            contentPadding = contentPadding
        ) {
            Text(text = headline, style = headlineStyle)
        }
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
