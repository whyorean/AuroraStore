/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
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
    AuroraListItem(
        modifier = modifier,
        headline = searchSuggestEntry.title,
        onClick = { onClick(searchSuggestEntry.title) },
        leading = {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(
                        if (searchSuggestEntry.hasImageContainer()) {
                            searchSuggestEntry.imageContainer.imageUrl
                        } else {
                            R.drawable.ic_search_suggestion
                        }
                    )
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                placeholder = painterResource(R.drawable.ic_search_suggestion),
                contentScale = ContentScale.Crop,
                modifier = Modifier.requiredSize(dimensionResource(R.dimen.icon_size_default))
            )
        },
        trailing = {
            IconButton(onClick = { onAction(searchSuggestEntry.title) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_search_append),
                    contentDescription = null
                )
            }
        }
    )
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun SearchSuggestionListItemPreview() {
    SearchSuggestionListItem(
        searchSuggestEntry = SearchSuggestEntry.getDefaultInstance()
    )
}
