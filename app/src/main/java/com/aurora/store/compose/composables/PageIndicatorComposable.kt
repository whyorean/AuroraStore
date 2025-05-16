/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composables

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.aurora.store.R

/**
 * Pager indicator
 * Adapted from [Pager in Compose](https://developer.android.com/jetpack/compose/layouts/pager#add-page)
 * @param modifier The modifier to be applied to the composable
 * @param totalPages Total number of pages
 * @param currentPage Currently displayed page number
 */
@Composable
fun PageIndicatorComposable(
    modifier: Modifier = Modifier,
    totalPages: Int,
    currentPage: Int = 0,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            dimensionResource(R.dimen.margin_xsmall),
            Alignment.CenterHorizontally
        )
    ) {
        repeat(totalPages) { iteration ->
            val isSelected = currentPage == iteration
            val color by animateColorAsState(
                targetValue = if (isSelected) {
                    Color.DarkGray
                } else {
                    Color.LightGray
                },
                animationSpec = tween()
            )
            val size by animateDpAsState(
                targetValue = if (isSelected) {
                    dimensionResource(R.dimen.radius_normal)
                } else {
                    dimensionResource(R.dimen.radius_small)
                },
                animationSpec = tween()
            )

            Box(
                modifier = modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(color = color)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PageIndicatorComposablePreview() {
    PageIndicatorComposable(totalPages = 5)
}
