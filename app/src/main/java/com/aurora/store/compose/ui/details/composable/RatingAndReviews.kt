/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.util.fastForEach
import com.aurora.extensions.isWindowCompact
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.Rating
import com.aurora.gplayapi.data.models.Review
import com.aurora.store.R
import com.aurora.store.compose.composable.Header
import com.aurora.store.compose.composable.details.RatingListItem
import com.aurora.store.compose.composable.details.ReviewListItem
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.PreviewTemplate
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
fun RatingAndReviews(
    rating: Rating,
    featuredReviews: List<Review> = emptyList(),
    onNavigateToDetailsReview: () -> Unit = {},
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo()
) {
    val stars = listOf(
        rating.oneStar,
        rating.twoStar,
        rating.threeStar,
        rating.fourStar,
        rating.fiveStar
    ).map { it.toFloat() }.also {
        // No ratings available, nothing to show
        if (it.sum() == 0F) return
    }

    val avgRating = when {
        windowAdaptiveInfo.isWindowCompact -> {
            String.format(Locale.getDefault(), "%.1f", rating.average)
        }

        else -> {
            String.format(Locale.getDefault(), "%.1f / 5.0", rating.average)
        }
    }

    Header(
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
                RatingListItem(
                    label = (stars.indexOf(star) + 1).toString(),
                    rating = star / stars.sum()
                )
            }
        }
    }

    if (featuredReviews.isNotEmpty()) {
        val pagerState = rememberPagerState { featuredReviews.size }
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(dimensionResource(R.dimen.padding_large)),
            pageSpacing = dimensionResource(R.dimen.margin_medium)
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.radius_small)))
                    .background(color = MaterialTheme.colorScheme.surfaceContainer)
                    .requiredHeight(dimensionResource(R.dimen.review_height))
            ) {
                ReviewListItem(review = featuredReviews[page])
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RatingAndReviewsPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    val reviews = List(3) {
        Review(
            userName = "Rahul Kumar Patel",
            timeStamp = 1745750879,
            rating = 4,
            comment = LoremIpsum(40).values.first()
        )
    }
    PreviewTemplate {
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_medium))
        ) {
            RatingAndReviews(rating = app.rating, featuredReviews = reviews)
        }
    }
}
