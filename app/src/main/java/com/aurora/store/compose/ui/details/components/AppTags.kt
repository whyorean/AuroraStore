/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details.components

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.compose.composables.app.AppTagComposable
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.util.CommonUtil

/**
 * Composable to display tags related to the app, supposed to be used as a part
 * of the Column with proper vertical arrangement spacing in the AppDetailsScreen.
 * @param app App to show tags
 */
@Composable
fun AppTags(app: App) {
    val context = LocalContext.current

    val installsLabel = CommonUtil.addDiPrefix(app.installs)
    val averageRating = if (app.labeledRating == "0.0" || app.labeledRating.isBlank()) {
        null
    } else {
        app.labeledRating
    }
    val paidLabel = if (app.isFree) {
        stringResource(R.string.details_free)
    } else {
        stringResource(R.string.details_paid)
    }
    val adsLabel = if (app.containsAds) {
        stringResource(R.string.details_contains_ads)
    } else {
        stringResource(R.string.details_no_ads)
    }

    val tags = mapOf(
        averageRating to R.drawable.ic_star,
        installsLabel to R.drawable.ic_download_manager,
        Formatter.formatShortFileSize(context, app.size) to R.drawable.ic_apk_install,
        app.updatedOn to R.drawable.ic_updates,
        paidLabel to R.drawable.ic_paid,
        adsLabel to R.drawable.ic_campaign,
    ).filterKeys { it != null }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_medium))
    ) {
        items(items = tags.keys.toList()) { label ->
            AppTagComposable(label = label!!, icon = tags.getValue(label))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppTagsPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_medium))) {
        AppTags(app = app)
    }
}
