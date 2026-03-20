/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details.composable

import android.text.format.Formatter
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.LayoutDirection
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.compose.composable.app.AnimatedAppIcon
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.PreviewTemplate
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
    onNavigateToDetailsDevProfile: (developerName: String) -> Unit = {}
) {
    val context = LocalContext.current
    val versionName = if (state is AppState.Installed) state.versionName else app.versionName
    val versionCode = if (state is AppState.Installed) state.versionCode else app.versionCode
    val speed = if (state is AppState.Downloading) state.speed else 0
    val timeRemaining = if (state is AppState.Downloading) state.timeRemaining else 0

    @Composable
    fun UpdatableVersion() {
        val updateVersion = stringResource(R.string.version, versionName, versionCode)
        val localVersion = stringResource(
            R.string.version,
            PackageUtil.getInstalledVersionName(context, app.packageName),
            PackageUtil.getInstalledVersionCode(context, app.packageName)
        )

        Text(
            style = MaterialTheme.typography.bodySmall,
            text = buildAnnotatedString {
                when (LocalLayoutDirection.current) {
                    LayoutDirection.Ltr -> append(localVersion)
                    LayoutDirection.Rtl -> append(updateVersion)
                }

                append(" ")
                appendInlineContent("iconId", "[arrow]")
                append(" ")

                when (LocalLayoutDirection.current) {
                    LayoutDirection.Ltr -> append(updateVersion)
                    LayoutDirection.Rtl -> append(localVersion)
                }
            },
            inlineContent = mapOf(
                "iconId" to InlineTextContent(
                    Placeholder(
                        width = MaterialTheme.typography.bodySmall.fontSize,
                        height = MaterialTheme.typography.bodySmall.fontSize,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_forward),
                        contentDescription = null
                    )
                }
            )
        )
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        AnimatedAppIcon(
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
                if (cState == AppState.Updatable::class) {
                    UpdatableVersion()
                    return@AnimatedContent
                }

                Text(
                    style = MaterialTheme.typography.bodySmall,
                    text = when (cState) {
                        AppState.Installing::class,
                        AppState.Downloading::class -> {
                            "${Formatter.formatShortFileSize(context, speed)}/s" +
                                ", " + CommonUtil.getETAString(context, timeRemaining)
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
    PreviewTemplate {
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_medium))
        ) {
            Details(app = app)
        }
    }
}
