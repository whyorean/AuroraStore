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
internal fun rememberShimmerBrush(): Brush {
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
private fun ShimmerBlock(
    modifier: Modifier,
    brush: Brush,
    radiusRes: Int = R.dimen.radius_small
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(dimensionResource(radiusRes)))
            .background(brush)
    )
}

@Composable
private fun ShimmerTextStack(
    modifier: Modifier = Modifier,
    widthFractions: List<Float>,
    brush: Brush
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xsmall))
    ) {
        widthFractions.forEach { fraction ->
            ShimmerBlock(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(14.dp),
                brush = brush
            )
        }
    }
}

@Composable
private fun ShimmerListRow(showTrailing: Boolean, sharedBrush: Brush? = null) {
    val brush = sharedBrush ?: rememberShimmerBrush()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.spacing_large),
                vertical = dimensionResource(R.dimen.spacing_xsmall)
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
    ) {
        ShimmerBlock(
            modifier = Modifier.requiredSize(dimensionResource(R.dimen.icon_size_medium)),
            brush = brush
        )
        ShimmerTextStack(
            modifier = Modifier.weight(1f),
            widthFractions = listOf(0.7f, 0.5f, 0.5f),
            brush = brush
        )
        if (showTrailing) {
            ShimmerBlock(
                modifier = Modifier
                    .width(80.dp)
                    .height(36.dp),
                brush = brush,
                radiusRes = R.dimen.radius_large
            )
        }
    }
}

@Composable
internal fun ShimmerAppRow(sharedBrush: Brush? = null) {
    ShimmerListRow(showTrailing = false, sharedBrush = sharedBrush)
}

@Composable
internal fun ShimmerUpdateItem(sharedBrush: Brush? = null) {
    ShimmerListRow(showTrailing = true, sharedBrush = sharedBrush)
}

@Composable
internal fun ShimmerCategoryRow(sharedBrush: Brush? = null) {
    val brush = sharedBrush ?: rememberShimmerBrush()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.spacing_large),
                vertical = dimensionResource(R.dimen.spacing_small)
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_large))
    ) {
        ShimmerBlock(
            modifier = Modifier.requiredSize(dimensionResource(R.dimen.icon_size_category)),
            brush = brush
        )
        ShimmerBlock(
            modifier = Modifier
                .weight(1f)
                .height(24.dp),
            brush = brush
        )
    }
}

@Composable
internal fun ShimmerCarouselSection(sharedBrush: Brush? = null) {
    val brush = sharedBrush ?: rememberShimmerBrush()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.spacing_large),
                vertical = dimensionResource(R.dimen.spacing_small)
            ),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
    ) {
        ShimmerBlock(
            modifier = Modifier
                .fillMaxWidth(0.42f)
                .height(16.dp),
            brush = brush
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
        ) {
            repeat(3) {
                ShimmerAppListItem(brush)
            }
        }
    }
}

@Composable
internal fun ShimmerAppListItem(sharedBrush: Brush? = null) {
    val brush = sharedBrush ?: rememberShimmerBrush()
    Column(
        modifier = Modifier
            .width(164.dp)
            .height(184.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(brush)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ShimmerBlock(
            modifier = Modifier.size(56.dp),
            brush = brush,
            radiusRes = R.dimen.radius_large
        )
        ShimmerBlock(
            modifier = Modifier
                .fillMaxWidth(0.84f)
                .height(14.dp),
            brush = brush
        )
        ShimmerBlock(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(11.dp),
            brush = brush
        )
        ShimmerBlock(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .height(11.dp),
            brush = brush
        )
    }
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
