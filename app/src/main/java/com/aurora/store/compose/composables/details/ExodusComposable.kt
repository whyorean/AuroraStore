/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composables.details

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.aurora.extensions.bodyVerySmall
import com.aurora.store.R
import com.aurora.store.data.model.ExodusTracker

/**
 * Composable to display details about a tracker reported by Exodus Privacy
 * @param modifier The modifier to be applied to the composable
 * @param tracker Tracker to display details about
 */
@Composable
fun ExodusComposable(modifier: Modifier = Modifier, tracker: ExodusTracker) {
    Column(modifier = modifier.padding(dimensionResource(R.dimen.padding_small))) {
        Text(
            text = tracker.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = tracker.signature,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = tracker.date,
            style = MaterialTheme.typography.bodyVerySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ExodusComposablePreview() {
    ExodusComposable(
        tracker = ExodusTracker(
            name = "Google Analytics",
            signature = "com.google.android.apps.analytics.|com.google.android.gms.analytics.|com.google.analytics.",
            date = "2017-09-24"
        )
    )
}
