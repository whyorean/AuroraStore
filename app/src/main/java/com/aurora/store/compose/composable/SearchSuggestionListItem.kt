/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import coil3.compose.AsyncImage
import com.aurora.gplayapi.SearchSuggestEntry
import com.aurora.store.R
import com.aurora.store.compose.preview.ThemePreviewProvider

@Composable
fun SearchSuggestionListItem(
    modifier: Modifier = Modifier,
    searchSuggestEntry: SearchSuggestEntry,
    onClick: (query: String) -> Unit = {},
    onAction: (query: String) -> Unit = {}
) {
    ListItem(
        onClick = { onClick(searchSuggestEntry.title) },
        modifier = modifier,
        shapes = auroraListItemShapes(),
        leadingContent = {
            AsyncImage(
                model = rememberStoreImageRequest(
                    if (searchSuggestEntry.hasImageContainer()) {
                        searchSuggestEntry.imageContainer.imageUrl
                    } else {
                        R.drawable.ic_search_suggestion
                    }
                ),
                contentDescription = null,
                placeholder = painterResource(R.drawable.ic_search_suggestion),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .requiredSize(dimensionResource(R.dimen.icon_size_default))
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.app_icon_radius)))
            )
        },
        trailingContent = {
            IconButton(onClick = { onAction(searchSuggestEntry.title) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_search_append),
                    contentDescription = null
                )
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        ),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(
            horizontal = dimensionResource(R.dimen.spacing_large),
            vertical = dimensionResource(R.dimen.spacing_small)
        )
    ) {
        Text(
            text = searchSuggestEntry.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun SearchSuggestionListItemPreview() {
    SearchSuggestionListItem(
        searchSuggestEntry = SearchSuggestEntry.getDefaultInstance()
    )
}
