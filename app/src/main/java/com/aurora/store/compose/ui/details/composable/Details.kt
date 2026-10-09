/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details.composable

import android.text.format.Formatter
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.compose.composable.app.AnimatedAppIcon
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.ThemePreviewProvider
import com.aurora.store.data.model.AppState
import com.aurora.store.util.CommonUtil

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
    showAppName: Boolean = true,
    onNavigateToDetailsDevProfile: (developerName: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val speed = if (state is AppState.Downloading) state.speed else 0
    val timeRemaining = if (state is AppState.Downloading) state.timeRemaining else 0

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium)),
        verticalAlignment = Alignment.Top
    ) {
        AnimatedAppIcon(
            modifier = Modifier.requiredSize(dimensionResource(R.dimen.icon_size_large)),
            iconUrl = app.iconArtwork.url,
            inProgress = state.inProgress(),
            progress = state.progress()
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            if (showAppName) {
                Text(
                    text = app.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .clickable(
                        role = Role.Button,
                        onClick = { onNavigateToDetailsDevProfile(app.developerName) }
                    )
                    .heightIn(min = 36.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Text(
                    text = app.developerName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (state.inProgress()) {
                AnimatedContent(
                    targetState = state::class,
                    transitionSpec = {
                        fadeIn(animationSpec = spring()) togetherWith
                            fadeOut(animationSpec = tween(durationMillis = 100))
                    },
                    label = "AppProgress"
                ) { currentState ->
                    Text(
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        text = when (currentState) {
                            AppState.Downloading::class -> {
                                "${Formatter.formatShortFileSize(context, speed)}/s" +
                                    ", " + CommonUtil.getETAString(context, timeRemaining)
                            }

                            AppState.Installing::class ->
                                stringResource(R.string.action_installing)

                            AppState.Queued::class -> stringResource(R.string.status_queued)

                            AppState.Purchasing::class ->
                                stringResource(R.string.preparing_to_download)

                            else -> stringResource(R.string.verifying_downloads)
                        }
                    )
                }
            }
        }
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun DetailsPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
    ) {
        Details(app = app)
    }
}
