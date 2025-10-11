/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details.components

import android.text.format.Formatter
import androidx.compose.animation.AnimatedContent
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
import coil3.compose.LocalAsyncImagePreviewHandler
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.compose.composables.app.AnimatedAppIconComposable
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.coilPreviewProvider
import com.aurora.store.data.model.AppState
import com.aurora.store.util.CommonUtil
import com.aurora.store.util.PackageUtil

/**
 * Composable to display basic app details, supposed to be used as a part
 * of the Column with proper vertical arrangement spacing in the AppDetailsScreen.
 * @param app App to show details about
 * @param state State of the app
 * @param onNavigateToDetailsDevProfile Callback when the developer name is tapped
 */
@Composable
fun Details(
    app: App,
    state: AppState = AppState.Unavailable,
    onNavigateToDetailsDevProfile: (developerName: String) -> Unit = {},
) {
    val context = LocalContext.current
    val versionName = if (state is AppState.Installed) state.versionName else app.versionName
    val versionCode = if (state is AppState.Installed) state.versionCode else app.versionCode
    val speed = if (state is AppState.Downloading) state.speed else 0
    val timeRemaining = if (state is AppState.Downloading) state.timeRemaining else 0

    Row(modifier = Modifier.fillMaxWidth()) {
        AnimatedAppIconComposable(
            modifier = Modifier.requiredSize(dimensionResource(R.dimen.icon_size_large)),
            iconUrl = app.iconArtwork.url,
            inProgress = state.inProgress(),
            progress = state.progress()
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
            AnimatedContent(targetState = state::class) { cState ->
                Text(
                    style = MaterialTheme.typography.bodySmall,
                    text = when (cState) {
                        AppState.Installing::class,
                        AppState.Downloading::class -> {
                            "${Formatter.formatShortFileSize(context, speed)}/s" +
                                    ", " + CommonUtil.getETAString(context, timeRemaining)
                        }

                        AppState.Updatable::class -> {
                            stringResource(
                                R.string.version_update,
                                PackageUtil.getInstalledVersionName(context, app.packageName),
                                PackageUtil.getInstalledVersionCode(context, app.packageName),
                                versionName,
                                versionCode
                            )
                        }

                        AppState.Queued::class -> stringResource(R.string.status_queued)
                        AppState.Purchasing::class -> stringResource(R.string.preparing_to_install)

                        else -> {
                            stringResource(R.string.version, versionName, versionCode)
                        }
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DetailsPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_medium))) {
        CompositionLocalProvider(LocalAsyncImagePreviewHandler provides coilPreviewProvider) {
            Details(app =  app)
        }
    }
}
