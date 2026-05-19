/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.aurora.store.R
import com.aurora.store.compose.preview.ThemePreviewProvider

/**
 * Section header row with optional right-chevron for navigation (replaces HeaderView).
 * When [browseUrl] is non-null and non-blank the row is clickable and shows a chevron.
 */
@Composable
fun SectionHeader(
    modifier: Modifier = Modifier,
    title: String,
    browseUrl: String? = null,
    onClick: () -> Unit = {}
) {
    val hasLink = !browseUrl.isNullOrBlank()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (hasLink) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(all = dimensionResource(R.dimen.padding_medium)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (hasLink) {
            Spacer(Modifier.width(dimensionResource(R.dimen.margin_small)))
            Icon(
                painter = painterResource(R.drawable.ic_arrow_right),
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(R.dimen.icon_size_default)),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Section header row with a right-aligned action button (replaces UpdateHeaderView).
 */
@Composable
fun SectionHeaderWithAction(
    modifier: Modifier = Modifier,
    title: String,
    action: String,
    onAction: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = dimensionResource(R.dimen.padding_small),
                top = dimensionResource(R.dimen.padding_xxsmall),
                bottom = dimensionResource(R.dimen.padding_xxsmall)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onAction) {
            Text(action)
        }
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun SectionHeaderPreview() {
    SectionHeader(title = "Top Charts", browseUrl = "https://example.com")
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun SectionHeaderWithActionPreview() {
    SectionHeaderWithAction(title = "3 updates available", action = "Update all")
}
