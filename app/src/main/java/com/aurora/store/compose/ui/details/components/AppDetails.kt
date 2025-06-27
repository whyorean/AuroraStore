/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.LocalAsyncImagePreviewHandler
import com.aurora.extensions.bodyVerySmall
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.compose.composables.app.AnimatedAppIconComposable
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.coilPreviewProvider
import com.aurora.store.util.PackageUtil

/**
 * Composable to display basic app details, supposed to be used as a part
 * of the Column with proper vertical arrangement spacing in the AppDetailsScreen.
 * @param app App to show details about
 * @param progress Ongoing progress percentage out of 100, for e.g. 50.0
 * @param inProgress Whether there is some ongoing progress related to the app
 * @param onNavigateToDetailsDevProfile Callback when the developer name is tapped
 * @param isUpdatable Whether the app has a valid update available
 */
@Composable
fun AppDetails(
    app: App,
    progress: Float = 0F,
    inProgress: Boolean = false,
    onNavigateToDetailsDevProfile: (developerName: String) -> Unit = {},
    isUpdatable: Boolean = false,
) {
    val context = LocalContext.current

    Row(modifier = Modifier.fillMaxWidth()) {
        AnimatedAppIconComposable(
            modifier = Modifier.requiredSize(dimensionResource(R.dimen.icon_size_large)),
            iconUrl = app.iconArtwork.url,
            inProgress = inProgress,
            progress = progress
        )
        Column(modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.margin_small))) {
            Text(
                text = app.displayName,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                modifier = Modifier
                    .clickable(onClick = { onNavigateToDetailsDevProfile(app.developerName) }),
                text = app.developerName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (!isUpdatable) {
                    stringResource(R.string.version, app.versionName, app.versionCode)
                } else {
                    stringResource(
                        R.string.version_update,
                        PackageUtil.getInstalledVersionName(context, app.packageName),
                        PackageUtil.getInstalledVersionCode(context, app.packageName),
                        app.versionName,
                        app.versionCode
                    )
                },
                style = MaterialTheme.typography.bodyVerySmall
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
@OptIn(ExperimentalCoilApi::class)
private fun AppDetailsPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_medium))) {
        CompositionLocalProvider(LocalAsyncImagePreviewHandler provides coilPreviewProvider) {
            AppDetails(app =  app)
        }
    }
}
