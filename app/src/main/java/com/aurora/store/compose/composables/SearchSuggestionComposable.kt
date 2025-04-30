/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.aurora.gplayapi.SearchSuggestEntry
import com.aurora.store.R

/**
 * Composable for displaying search suggestions in a list
 * @param modifier The modifier to be applied to the composable
 * @param searchSuggestEntry A [SearchSuggestEntry] to display search suggestion
 * @param onClick Callback when this composable is clicked
 * @param onAction Callback when action button is clicked
 */
@Composable
fun SearchSuggestionComposable(
    modifier: Modifier = Modifier,
    searchSuggestEntry: SearchSuggestEntry,
    onClick: () -> Unit = {},
    onAction: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(dimensionResource(R.dimen.padding_medium)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .weight(1F)
                .clickable(onClick = onClick),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_medium))
        ) {
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
            Text(
                text = searchSuggestEntry.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onAction) {
            Icon(
                painter = painterResource(R.drawable.ic_search_append),
                contentDescription = null
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchSuggestionComposablePreview() {
    SearchSuggestionComposable(
        searchSuggestEntry = SearchSuggestEntry.getDefaultInstance()
    )
}
