/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composables.details

import android.text.format.DateUtils
import android.widget.RatingBar
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.viewinterop.AndroidView
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImage
import coil3.compose.LocalAsyncImagePreviewHandler
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.aurora.gplayapi.data.models.Review
import com.aurora.store.R
import com.aurora.store.compose.preview.ReviewPreviewProvider
import com.aurora.store.compose.preview.coilPreviewProvider

/**
 * Composable for viewing a review about an app
 * @param modifier The modifier to be applied to the composable
 * @param review [Review] about an app
 */
@Composable
fun ReviewComposable(modifier: Modifier = Modifier, review: Review) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.padding_medium),
                vertical = dimensionResource(R.dimen.padding_small)
            )
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
        Column(
            modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.margin_small)),
        ) {
            Text(
                text = review.userName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = DateUtils.formatDateTime(
                    LocalContext.current,
                    review.timeStamp,
                    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR
                ),
                style = MaterialTheme.typography.bodySmall
            )
            AndroidView(
                factory = { context ->
                    RatingBar(context, null, android.R.attr.ratingBarStyleSmall)
                },
                update = { view ->
                    view.rating = review.rating.toFloat()
                }
            )
            Text(
                text = review.comment,
                style = MaterialTheme.typography.bodySmall,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
@OptIn(ExperimentalCoilApi::class)
private fun ReviewComposablePreview(@PreviewParameter(ReviewPreviewProvider::class) review: Review) {
    CompositionLocalProvider(LocalAsyncImagePreviewHandler provides coilPreviewProvider) {
        ReviewComposable(review = review)
    }
}
