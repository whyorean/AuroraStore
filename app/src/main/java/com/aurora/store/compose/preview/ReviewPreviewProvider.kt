/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import com.aurora.gplayapi.data.models.Review

/**
 * Preview provider for composable working with [Review]
 */
class ReviewPreviewProvider : PreviewParameterProvider<Review> {

    override val values: Sequence<Review>
        get() = sequenceOf(
            Review(
                userName = "Rahul Kumar Patel",
                timeStamp = 1745750879,
                rating = 4,
                comment = LoremIpsum(40).values.first()
            )
        )
}
