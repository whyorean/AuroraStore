/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.util.fastForEach
import androidx.paging.compose.LazyPagingItems
import androidx.window.core.layout.WindowWidthSizeClass
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.Rating
import com.aurora.gplayapi.data.models.Review
import com.aurora.store.R
import com.aurora.store.compose.composables.HeaderComposable
import com.aurora.store.compose.composables.PageIndicatorComposable
import com.aurora.store.compose.composables.details.RatingComposable
import com.aurora.store.compose.composables.details.ReviewComposable
import com.aurora.store.compose.composables.preview.AppPreviewProvider
import java.util.Locale

/**
 * Composable to display reviews of the app, supposed to be used as a part
 * of the Column with proper vertical arrangement spacing in the AppDetailsScreen.
 * @param rating Rating of the app
 * @param featuredReviews Featured app reviews
 * @param onNavigateToDetailsReview Callback when the user navigates
 * @param windowAdaptiveInfo Adaptive window information
 */
@Composable
fun AppRatingAndReviews(
    rating: Rating,
    featuredReviews: List<Review> = emptyList(),
    onNavigateToDetailsReview: () -> Unit = {},
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo()
) {
    val stars = listOf(
        rating.oneStar, rating.twoStar, rating.threeStar, rating.fourStar, rating.fiveStar
    ).map { it.toFloat() }.also {
        // No ratings available, nothing to show
        if (it.sum() == 0F) return
    }

    val avgRating = when (windowAdaptiveInfo.windowSizeClass.windowWidthSizeClass) {
        WindowWidthSizeClass.COMPACT -> String.format(Locale.getDefault(), "%.1f", rating.average)
        else -> String.format(Locale.getDefault(), "%.1f / 5.0", rating.average)
    }

    HeaderComposable(
        title = stringResource(R.string.details_ratings),
        onClick = onNavigateToDetailsReview
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_small))
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.padding_medium)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = avgRating,
                maxLines = 1,
                style = MaterialTheme.typography.displayMedium,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = rating.abbreviatedLabel,
                maxLines = 1,
                style = MaterialTheme.typography.bodySmall,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.padding_medium)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_small))
        ) {
            stars.reversed().fastForEach { star ->
                RatingComposable(
                    label = (stars.indexOf(star) + 1).toString(),
                    rating = star / stars.sum()
                )
            }
        }
    }

    if (featuredReviews.isNotEmpty()) {
        val pagerState = rememberPagerState { featuredReviews.size }
        HorizontalPager(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.padding_medium)),
            state = pagerState,
            pageSpacing = dimensionResource(R.dimen.margin_normal)
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.radius_small)))
                    .background(color = MaterialTheme.colorScheme.secondaryContainer)
                    .requiredHeight(dimensionResource(R.dimen.review_height))
            ) {
                ReviewComposable(review = featuredReviews[page])
            }
        }

        PageIndicatorComposable(
            totalPages = featuredReviews.size,
            currentPage = pagerState.currentPage
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppReviewsPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_medium))) {
        AppRatingAndReviews(rating = app.rating)
    }
}
