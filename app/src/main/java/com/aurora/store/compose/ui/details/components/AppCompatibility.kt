/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import com.aurora.store.R
import com.aurora.store.compose.composables.HeaderComposable
import com.aurora.store.compose.composables.InfoComposable
import com.aurora.store.data.model.Scores

/**
 * Composable to display app compatibility rating from Plexus, supposed to be used as a part
 * of the Column with proper vertical arrangement spacing in the AppDetailsScreen.
 * @param needsGms Whether this app needs Google Play Services to work properly
 * @param plexusScores Scores from the Plexus
 */
@Composable
fun AppCompatibility(needsGms: Boolean, plexusScores: Scores? = null) {
    HeaderComposable(
        title = stringResource(R.string.details_compatibility_title),
        subtitle = stringResource(R.string.plexus_powered),
    )

    if (!needsGms) {
        InfoComposable(
            icon = R.drawable.ic_menu_about,
            title = AnnotatedString(
                text = stringResource(R.string.details_compatibility_gms_not_required_title)
            ),
            description = AnnotatedString(
                text = stringResource(R.string.details_compatibility_gms_not_required_subtitle)
            )
        )

        // Nothing more to show
        return
    }

    InfoComposable(
        icon = R.drawable.ic_menu_about,
        title = AnnotatedString(
            text = stringResource(R.string.details_compatibility_gms_required_title)
        ),
        description = AnnotatedString(
            text = stringResource(R.string.details_compatibility_gms_required_subtitle)
        )
    )

    val scoresStatus = mapOf(
        R.string.details_compatibility_no_gms to plexusScores?.aosp?.status,
        R.string.details_compatibility_microg to plexusScores?.microG?.status,
    )
    scoresStatus.forEach { (title, description) ->
        InfoComposable(
            icon = R.drawable.ic_android,
            title = AnnotatedString(text = stringResource(title)),
            description = AnnotatedString(
                text = stringResource(description ?: R.string.details_compatibility_status_unknown)
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppCompatibilityPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_medium))) {
        AppCompatibility(needsGms = true)
    }
}
