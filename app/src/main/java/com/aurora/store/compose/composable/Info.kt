/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.ThemePreviewProvider

/**
 * Read-only or clickable information row rendered with the Material 3 ListItem component.
 */
@Composable
fun Info(
    modifier: Modifier = Modifier,
    title: AnnotatedString,
    description: AnnotatedString? = null,
    painter: Painter? = null,
    titleColor: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
    horizontalPadding: Dp = 0.dp
) {
    val itemModifier = modifier
        .padding(horizontal = horizontalPadding)
    val leadingContent: (@Composable () -> Unit)? = painter?.let { icon ->
        {
            Surface(
                modifier = Modifier.size(dimensionResource(R.dimen.icon_size_small)),
                shape = RoundedCornerShape(dimensionResource(R.dimen.app_icon_radius)),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(dimensionResource(R.dimen.icon_size_category))
                    )
                }
            }
        }
    }
    val supportingContent: (@Composable () -> Unit)? = description
        ?.takeIf { it.isNotBlank() }
        ?.let { content ->
            {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    val itemColors = ListItemDefaults.colors(containerColor = Color.Transparent)
    val contentPadding = PaddingValues(
        horizontal = dimensionResource(R.dimen.spacing_large),
        vertical = dimensionResource(R.dimen.spacing_small)
    )

    if (onClick != null) {
        ListItem(
            onClick = onClick,
            modifier = itemModifier,
            shapes = auroraListItemShapes(),
            supportingContent = supportingContent,
            leadingContent = leadingContent,
            colors = itemColors,
            contentPadding = contentPadding
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = titleColor
            )
        }
    } else {
        ListItem(
            modifier = itemModifier,
            enabled = true,
            shapes = auroraListItemShapes(),
            supportingContent = supportingContent,
            leadingContent = leadingContent,
            colors = itemColors,
            contentPadding = contentPadding
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = titleColor
            )
        }
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun InfoPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    Info(
        title = AnnotatedString(text = stringResource(R.string.details_dev_website)),
        description = AnnotatedString.fromHtml(htmlString = app.developerWebsite),
        painter = painterResource(R.drawable.ic_network)
    )
}
