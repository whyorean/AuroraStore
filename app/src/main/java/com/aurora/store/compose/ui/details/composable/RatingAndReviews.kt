/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.dp
import com.aurora.extensions.isWindowCompact
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.Rating
import com.aurora.gplayapi.data.models.Review
import com.aurora.store.R
import com.aurora.store.compose.composable.details.RatingListItem
import com.aurora.store.compose.composable.details.ReviewListItem
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.ThemePreviewProvider

/**
 * Displays a concise rating summary and an obvious portal to the full review list.
 */
@Composable
fun RatingAndReviews(
    rating: Rating,
    featuredReviews: List<Review> = emptyList(),
    onNavigateToDetailsReview: () -> Unit = {},
    showPortal: Boolean = true,
    showSummary: Boolean = true,
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfoV2()
) {
    val stars = remember(
        rating.oneStar,
        rating.twoStar,
        rating.threeStar,
        rating.fourStar,
        rating.fiveStar
    ) {
        listOf(
            rating.oneStar,
            rating.twoStar,
            rating.threeStar,
            rating.fourStar,
            rating.fiveStar
        ).map { it.toFloat() }
    }
    val totalRatings = remember(stars) { stars.sum() }
    val hasRatingData = totalRatings > 0F

    if (!hasRatingData && featuredReviews.isEmpty()) return

    val locale = LocalLocale.current.platformLocale
    val isCompactWindow = windowAdaptiveInfo.isWindowCompact
    val avgRating = if (hasRatingData) {
        remember(rating.average, isCompactWindow, locale) {
            if (isCompactWindow) {
                String.format(locale, "%.1f", rating.average)
            } else {
                String.format(locale, "%.1f / 5.0", rating.average)
            }
        }
    } else {
        null
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (showSummary) {
            if (showPortal) {
                RatingPreviewCard(
                    rating = rating,
                    stars = stars,
                    totalRatings = totalRatings,
                    avgRating = avgRating,
                    featuredReview = featuredReviews.firstOrNull(),
                    hasRatingData = hasRatingData
                )
            } else {
                if (hasRatingData && avgRating != null) {
                    RatingSummaryCard(
                        rating = rating,
                        stars = stars,
                        totalRatings = totalRatings,
                        avgRating = avgRating
                    )
                }
                if (featuredReviews.isNotEmpty()) {
                    FeaturedReviewsPager(featuredReviews = featuredReviews)
                }
            }
        }

        if (showPortal) {
            DetailsPortalCard(
                title = stringResource(R.string.details_ratings),
                description = stringResource(R.string.details_ratings_portal_description),
                icon = painterResource(R.drawable.ic_star_filled),
                onClick = onNavigateToDetailsReview
            )
        }
    }
}

/** Keeps the rating distribution and its featured review together as one emphasized group. */
@Composable
private fun RatingPreviewCard(
    rating: Rating,
    stars: List<Float>,
    totalRatings: Float,
    avgRating: String?,
    featuredReview: Review?,
    hasRatingData: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.spacing_large)),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column {
            if (hasRatingData && avgRating != null) {
                RatingBreakdown(
                    rating = rating,
                    stars = stars,
                    totalRatings = totalRatings,
                    avgRating = avgRating
                )
            }

            if (featuredReview != null) {
                if (hasRatingData) {
                    HorizontalDivider(
                        modifier = Modifier.padding(
                            horizontal = dimensionResource(R.dimen.spacing_large)
                        ),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
                FeaturedReviewContent(review = featuredReview)
            }
        }
    }
}

@Composable
private fun FeaturedReviewContent(review: Review) {
    Column {
        Text(
            modifier = Modifier.padding(
                start = dimensionResource(R.dimen.spacing_large),
                top = dimensionResource(R.dimen.spacing_large),
                end = dimensionResource(R.dimen.spacing_large)
            ),
            text = stringResource(R.string.details_featured_review),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ReviewListItem(
            modifier = Modifier.padding(bottom = 8.dp),
            review = review,
            maxCommentLines = 3,
            expandable = true
        )
    }
}

/** Rating overview used on the dedicated reviews page. */
@Composable
private fun RatingSummaryCard(
    rating: Rating,
    stars: List<Float>,
    totalRatings: Float,
    avgRating: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.spacing_large)),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        RatingBreakdown(
            rating = rating,
            stars = stars,
            totalRatings = totalRatings,
            avgRating = avgRating
        )
    }
}

@Composable
private fun RatingBreakdown(
    rating: Rating,
    stars: List<Float>,
    totalRatings: Float,
    avgRating: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_large))
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.spacing_small)),
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
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
        ) {
            stars.asReversed().forEachIndexed { index, star ->
                RatingListItem(
                    label = (5 - index).toString(),
                    rating = star / totalRatings
                )
            }
        }
    }
}

/** Full featured review pager retained on the dedicated reviews page. */
@Composable
private fun FeaturedReviewsPager(featuredReviews: List<Review>) {
    val pagerState = rememberPagerState { featuredReviews.size }
    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.spacing_large)),
        pageSpacing = dimensionResource(R.dimen.spacing_medium)
    ) { page ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(dimensionResource(R.dimen.review_height)),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            ReviewListItem(review = featuredReviews[page])
        }
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
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
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
    ) {
        RatingAndReviews(rating = app.rating, featuredReviews = reviews)
    }
}
