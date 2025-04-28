/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.window.core.layout.WindowWidthSizeClass
import com.aurora.extensions.adaptiveNavigationIcon
import com.aurora.gplayapi.data.models.Review
import com.aurora.store.R
import com.aurora.store.compose.composables.TopAppBarComposable
import com.aurora.store.compose.composables.details.ReviewComposable
import com.aurora.store.viewmodel.details.AppDetailsViewModel
import com.aurora.store.viewmodel.review.DetailsReviewViewModel
import kotlinx.coroutines.flow.flowOf

@Composable
fun DetailsReviewScreen(
    onNavigateUp: () -> Unit,
    appDetailsViewModel: AppDetailsViewModel = hiltViewModel(),
    detailsReviewViewModel: DetailsReviewViewModel = hiltViewModel { factory: DetailsReviewViewModel.Factory ->
        factory.create(appDetailsViewModel.app.value!!.packageName)
    },
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo()
) {
    val app by appDetailsViewModel.app.collectAsStateWithLifecycle()
    val reviews = detailsReviewViewModel.reviews.collectAsLazyPagingItems()

    val topAppBarTitle = when (windowAdaptiveInfo.windowSizeClass.windowWidthSizeClass) {
        WindowWidthSizeClass.COMPACT -> app!!.displayName
        else -> stringResource(R.string.details_ratings)
    }

    ScreenContent(
        topAppBarTitle = topAppBarTitle,
        reviews = reviews,
        onNavigateUp = onNavigateUp,
        onFilter = { filter -> detailsReviewViewModel.fetchReviews(filter) }
    )
}

@Composable
private fun ScreenContent(
    topAppBarTitle: String? = null,
    onNavigateUp: () -> Unit = {},
    reviews: LazyPagingItems<Review>,
    onFilter: (filter: Review.Filter) -> Unit = {},
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo()
) {

    Scaffold(
        topBar = {
            TopAppBarComposable(
                title = topAppBarTitle,
                navigationIcon = windowAdaptiveInfo.adaptiveNavigationIcon,
                onNavigateUp = onNavigateUp
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = dimensionResource(R.dimen.padding_medium))
        ) {
            FilterHeader { filter -> onFilter(filter) }

            // TODO: Implement loading and error screen
            when (reviews.loadState.refresh) {
                is LoadState.Loading -> {}

                is LoadState.Error -> {}

                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(
                            count = reviews.itemCount,
                            key = reviews.itemKey { it.commentId }
                        ) { index ->
                            reviews[index]?.let { review -> ReviewComposable(review = review) }
                        }
                    }
                }
            }
        }


    }
}

/**
 * Composable to hold sticky header for filtering through the reviews
 */
@Composable
private fun FilterHeader(onClick: (filter: Review.Filter) -> Unit) {
    var activeFilter by rememberSaveable { mutableStateOf(Review.Filter.ALL) }

    val filters = mapOf(
        Review.Filter.ALL to R.string.filter_review_all,
        Review.Filter.NEWEST to R.string.filter_latest,
        Review.Filter.CRITICAL to R.string.filter_review_critical,
        Review.Filter.POSITIVE to R.string.filter_review_positive,
        Review.Filter.FIVE to R.string.filter_review_five,
        Review.Filter.FOUR to R.string.filter_review_four,
        Review.Filter.THREE to R.string.filter_review_three,
        Review.Filter.TWO to R.string.filter_review_two,
        Review.Filter.ONE to R.string.filter_review_one
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_normal))
    ) {
        items(items = filters.keys.toList(), key = { item -> item }) { filter ->
            val selected = activeFilter == filter
            FilterChip(
                onClick = {
                    activeFilter = filter
                    onClick(filter)
                },
                label = { Text(text = stringResource(filters.getValue(filter))) },
                selected = selected,
                leadingIcon = {
                    if (selected) {
                        Icon(
                            imageVector = Icons.Filled.Done,
                            contentDescription = stringResource(filters.getValue(filter))
                        )
                    }
                },
            )
        }
    }
}

@Preview
@Composable
private fun DetailsReviewScreenPreview() {
    val reviews = flowOf(PagingData.empty<Review>()).collectAsLazyPagingItems()

    ScreenContent(
        reviews = reviews
    )
}
