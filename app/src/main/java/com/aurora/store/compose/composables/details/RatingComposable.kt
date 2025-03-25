/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composables.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.aurora.store.R

/**
 * Composable to show a progress bar with rating for an app
 * @param label Label of the rating, for e.g. 5
 * @param rating Current rating, for e.g. 0.3
 */
@Composable
fun RatingComposable(label: String, rating: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.padding_small)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_small))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(R.dimen.radius_small)),
            progress = { rating }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RatingComposablePreview() {
    RatingComposable(label = "5", rating = 0.5F)
}
