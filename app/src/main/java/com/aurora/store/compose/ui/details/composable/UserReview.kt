/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details.composable

import android.text.format.DateUtils
import android.widget.RatingBar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.aurora.gplayapi.data.models.Review
import com.aurora.store.R
import com.aurora.store.compose.composable.SectionHeader
import com.aurora.store.compose.preview.ThemePreviewProvider

/**
 * Composable that lets a signed-in user rate and review an app. Once a review exists it shows a
 * read-only summary with an "Edit your review" action; otherwise (or while editing) it shows the
 * rating form. Meant to be used as a part of the Column with proper vertical arrangement spacing
 * in the AppDetailsScreen. Callers are responsible for only showing this to non-anonymous accounts
 * for installed apps.
 * @param review The user's existing review, used to pre-fill the form and drive the summary
 * @param onSubmit Callback invoked with the rating, title and comment when the user posts
 * @param onDelete Callback invoked when the user confirms deletion of their review
 */
@Composable
fun UserReview(
    review: Review? = null,
    onSubmit: (rating: Int, title: String, comment: String) -> Unit = { _, _, _ -> },
    onDelete: () -> Unit = {}
) {
    // Default to editing only when there is no review yet. Keying on whether a review exists flips
    // back to the summary once one is posted, while still letting "Edit" toggle within that state.
    var isEditing by rememberSaveable(review == null) { mutableStateOf(review == null) }

    if (review != null && !isEditing) {
        ReviewSummary(review = review, onEdit = { isEditing = true }, onDelete = onDelete)
    } else {
        ReviewForm(
            review = review,
            onSubmit = { rating, title, comment ->
                onSubmit(rating, title, comment)
                isEditing = false
            }
        )
    }
}

/**
 * Read-only view of the user's posted review with actions to edit or delete it.
 */
@Composable
private fun ReviewSummary(review: Review, onEdit: () -> Unit, onDelete: () -> Unit) {
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            title = { Text(text = stringResource(R.string.details_review_delete_title)) },
            text = { Text(text = stringResource(R.string.details_review_delete_message)) },
            onDismissRequest = { showDeleteDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text(text = stringResource(R.string.details_review_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            }
        )
    }

    SectionHeader(title = stringResource(R.string.details_your_review))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.spacing_medium)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(review.userPhotoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .requiredSize(dimensionResource(R.dimen.icon_size_small))
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.radius_medium)))
            )
            Column {
                Text(
                    text = review.userName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(
                        dimensionResource(R.dimen.spacing_xsmall)
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AndroidView(
                        factory = { context ->
                            RatingBar(
                                context,
                                null,
                                android.R.attr.ratingBarStyleIndicator
                            ).apply {
                                numStars = 5
                                stepSize = 1F
                                setIsIndicator(true)
                            }
                        },
                        update = { it.rating = review.rating.toFloat() }
                    )
                    if (review.timeStamp > 0L) {
                        Text(text = "·", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = DateUtils.formatDateTime(
                                LocalContext.current,
                                review.timeStamp,
                                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        if (review.title.isNotBlank()) {
            Text(text = review.title, style = MaterialTheme.typography.titleSmall)
        }

        if (review.comment.isNotBlank()) {
            Text(text = review.comment, style = MaterialTheme.typography.bodyMedium)
        }

        Row(
            modifier = Modifier.align(Alignment.End),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
        ) {
            TextButton(onClick = { showDeleteDialog = true }) {
                Text(text = stringResource(R.string.details_review_delete))
            }
            TextButton(onClick = onEdit) {
                Text(text = stringResource(R.string.details_review_edit))
            }
        }
    }
}

/**
 * Editable form for rating an app and writing an optional review.
 */
@Composable
private fun ReviewForm(
    review: Review?,
    onSubmit: (rating: Int, title: String, comment: String) -> Unit
) {
    // Key on the field values rather than the Review instance: Review.equals only compares
    // commentId, so keying on the instance would miss content changes synced from the Play Store.
    var rating by rememberSaveable(review?.rating) { mutableIntStateOf(review?.rating ?: 0) }
    var title by rememberSaveable(review?.title) { mutableStateOf(review?.title.orEmpty()) }
    var comment by rememberSaveable(review?.comment) { mutableStateOf(review?.comment.orEmpty()) }

    SectionHeader(title = stringResource(R.string.details_think_this_app))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.spacing_medium)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
    ) {
        AndroidView(
            factory = { context ->
                RatingBar(context, null, android.R.attr.ratingBarStyle).apply {
                    numStars = 5
                    stepSize = 1F
                    setOnRatingBarChangeListener { _, value, fromUser ->
                        if (fromUser) rating = value.toInt()
                    }
                }
            },
            update = { it.rating = rating.toFloat() }
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = title,
            onValueChange = { title = it },
            placeholder = { Text(text = stringResource(R.string.details_ratings_title_hint)) },
            singleLine = true,
            shape = RoundedCornerShape(dimensionResource(R.dimen.radius_medium))
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = comment,
            onValueChange = { comment = it },
            placeholder = { Text(text = stringResource(R.string.details_review_comment_hint)) },
            minLines = 3,
            shape = RoundedCornerShape(dimensionResource(R.dimen.radius_medium))
        )

        Button(
            modifier = Modifier.align(Alignment.End),
            onClick = { onSubmit(rating, title, comment) },
            enabled = rating > 0
        ) {
            Text(text = stringResource(R.string.action_post))
        }
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun UserReviewFormPreview() {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
    ) {
        UserReview()
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun UserReviewSummaryPreview() {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
    ) {
        UserReview(
            review = Review(
                title = "Great app",
                comment = "Works flawlessly without Google Play Services.",
                rating = 5
            )
        )
    }
}
