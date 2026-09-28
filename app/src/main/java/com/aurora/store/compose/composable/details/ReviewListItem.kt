/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable.details

import android.text.format.DateUtils
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewWrapper
import coil3.compose.AsyncImage
import com.aurora.gplayapi.data.models.Review
import com.aurora.store.R
import com.aurora.store.compose.composable.RoundedIconButton
import com.aurora.store.compose.composable.rememberStoreImageRequest
import com.aurora.store.compose.preview.ReviewPreviewProvider
import com.aurora.store.compose.preview.ThemePreviewProvider

/**
 * Composable for viewing a review about an app
 * @param modifier The modifier to be applied to the composable
 * @param review [Review] about an app
 */
@Composable
fun ReviewListItem(
    modifier: Modifier = Modifier,
    review: Review,
    maxCommentLines: Int = Int.MAX_VALUE,
    expandable: Boolean = false
) {
    var expanded by rememberSaveable(review.commentId, review.comment) {
        mutableStateOf(false)
    }
    var hasMoreContent by rememberSaveable(review.commentId, review.comment) {
        mutableStateOf(false)
    }

    ListItem(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (expandable) Modifier.animateContentSize(animationSpec = spring())
                else Modifier
            ),
        shapes = com.aurora.store.compose.composable.auroraListItemShapes(),
        overlineContent = {
            Text(
                text = DateUtils.formatDateTime(
                    LocalContext.current,
                    review.timeStamp,
                    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
            supportingContent = {
            Column(
                modifier = if (expandable) {
                    Modifier.animateContentSize(animationSpec = spring())
                } else {
                    Modifier
                }
            ) {
                StarRating(rating = review.rating)
                Text(
                    text = review.comment,
                    modifier = Modifier.padding(top = dimensionResource(R.dimen.spacing_small)),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (expandable && expanded) Int.MAX_VALUE else maxCommentLines,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { result ->
                        if (expandable && !expanded) {
                            hasMoreContent = result.didOverflowHeight
                        }
                    }
                )
            }
        },
        leadingContent = {
            AsyncImage(
                model = rememberStoreImageRequest(review.userPhotoUrl),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .requiredSize(dimensionResource(R.dimen.icon_size_small))
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.radius_medium)))
            )
        },
        trailingContent = {
            if (expandable && hasMoreContent) {
                RoundedIconButton(
                    onClick = { expanded = !expanded },
                    painter = painterResource(
                        if (expanded) R.drawable.ic_keyboard_arrow_up
                        else R.drawable.ic_keyboard_arrow_down
                    ),
                    contentDescription = stringResource(
                        if (expanded) R.string.details_collapse_review
                        else R.string.details_expand_review
                    )
                )
            }
        },
        verticalAlignment = Alignment.CenterVertically,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(
            horizontal = dimensionResource(R.dimen.spacing_large),
            vertical = dimensionResource(R.dimen.spacing_small)
        )
    ) {
        Text(
            text = review.userName,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun ReviewListItemPreview(@PreviewParameter(ReviewPreviewProvider::class) review: Review) {
    ReviewListItem(review = review)
}
