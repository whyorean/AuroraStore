/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import com.aurora.store.R
import com.aurora.store.compose.preview.ThemePreviewProvider

@Composable
fun shimmerBrush(): Brush {
    val base = MaterialTheme.colorScheme.onSurface
    val colors = listOf(
        base.copy(alpha = 0.08f),
        base.copy(alpha = 0.20f),
        base.copy(alpha = 0.08f)
    )
    val transition = rememberInfiniteTransition(label = "shimmer")
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )
    val x = offset * 1200f
    return Brush.linearGradient(
        colors = colors,
        start = Offset(x - 400f, 0f),
        end = Offset(x, 400f)
    )
}

@Composable
internal fun ShimmerCategoryRow() {
    val brush = shimmerBrush()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.padding_medium),
                vertical = dimensionResource(R.dimen.padding_small)
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_large))
    ) {
        Box(
            modifier = Modifier
                .requiredSize(dimensionResource(R.dimen.icon_size_category))
                .clip(RoundedCornerShape(dimensionResource(R.dimen.radius_small)))
                .background(brush)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
                .clip(RoundedCornerShape(dimensionResource(R.dimen.radius_small)))
                .background(brush)
        )
    }
}

@Composable
internal fun ShimmerAppRow() {
    val brush = shimmerBrush()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.padding_small),
                vertical = dimensionResource(R.dimen.padding_xsmall)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .requiredSize(dimensionResource(R.dimen.icon_size_medium))
                .clip(RoundedCornerShape(dimensionResource(R.dimen.radius_small)))
                .background(brush)
        )
        Column(
            modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.margin_small))
        ) {
            repeat(3) { i ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (i == 0) 0.75f else 0.5f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(dimensionResource(R.dimen.radius_small)))
                        .background(brush)
                )
                if (i < 2) Spacer(Modifier.height(dimensionResource(R.dimen.padding_xxsmall)))
            }
        }
    }
}

@Composable
internal fun ShimmerUpdateItem() {
    val brush = shimmerBrush()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.padding_small),
                vertical = dimensionResource(R.dimen.padding_xsmall)
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small))
    ) {
        Box(
            modifier = Modifier
                .requiredSize(dimensionResource(R.dimen.icon_size_medium))
                .clip(RoundedCornerShape(dimensionResource(R.dimen.radius_small)))
                .background(brush)
        )

        Column(modifier = Modifier.weight(1f)) {
            repeat(3) { i ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (i == 0) 0.7f else 0.5f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(dimensionResource(R.dimen.radius_small)))
                        .background(brush)
                )
                if (i < 2) Spacer(Modifier.height(dimensionResource(R.dimen.padding_xxsmall)))
            }
        }

        Box(
            modifier = Modifier
                .width(80.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(50))
                .background(brush)
        )
    }
}

@Composable
internal fun ShimmerCarouselSection() {
    val brush = shimmerBrush()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = dimensionResource(R.dimen.padding_small)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.42f)
                .height(16.dp)
                .clip(RoundedCornerShape(dimensionResource(R.dimen.radius_small)))
                .background(brush)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small))
        ) {
            repeat(5) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(
                        dimensionResource(R.dimen.padding_xsmall)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .size(dimensionResource(R.dimen.icon_size_cluster))
                            .clip(RoundedCornerShape(dimensionResource(R.dimen.radius_medium)))
                            .background(brush)
                    )
                    Box(
                        modifier = Modifier
                            .width(dimensionResource(R.dimen.icon_size_cluster) * 0.75f)
                            .height(11.dp)
                            .clip(RoundedCornerShape(dimensionResource(R.dimen.radius_small)))
                            .background(brush)
                    )
                    Box(
                        modifier = Modifier
                            .width(dimensionResource(R.dimen.icon_size_cluster) * 0.5f)
                            .height(11.dp)
                            .clip(RoundedCornerShape(dimensionResource(R.dimen.radius_small)))
                            .background(brush)
                    )
                }
            }
        }
    }
}

@Composable
internal fun ShimmerAppListItem() {
    val brush = shimmerBrush()
    Box(
        modifier = Modifier
            .padding(dimensionResource(R.dimen.padding_xsmall))
            .size(dimensionResource(R.dimen.icon_size_cluster))
            .clip(RoundedCornerShape(dimensionResource(R.dimen.radius_medium)))
            .background(brush)
    )
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun ShimmerCategoryRowPreview() {
    ShimmerCategoryRow()
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun ShimmerAppRowPreview() {
    ShimmerAppRow()
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun ShimmerUpdateItemPreview() {
    ShimmerUpdateItem()
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun ShimmerCarouselSectionPreview() {
    ShimmerCarouselSection()
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun ShimmerAppListItemPreview() {
    ShimmerAppListItem()
}
