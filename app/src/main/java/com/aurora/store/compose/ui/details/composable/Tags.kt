/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details.composable

import android.text.format.Formatter
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.snapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.ThemePreviewProvider
import com.aurora.store.data.model.AppState
import com.aurora.store.util.CommonUtil
import com.aurora.store.util.PackageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class AppMetric(val label: String, val value: String, val icon: Int)

/** Displays app metadata in a spacious, expressive Material 3 carousel. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Tags(app: App, state: AppState = AppState.Unavailable) {
    val context = LocalContext.current
    val averageRating = app.labeledRating.takeUnless { it == "0.0" || it.isBlank() }
    val updatedOn = app.updatedOn.orEmpty()
    val ratingLabel = stringResource(R.string.details_metric_rating)
    val versionLabel = stringResource(R.string.details_metric_version)
    val downloadsLabel = stringResource(R.string.details_metric_downloads)
    val sizeLabel = stringResource(R.string.details_metric_size)
    val updatedLabel = stringResource(R.string.details_metric_updated)
    val priceLabel = stringResource(R.string.details_metric_price)
    val priceValue = stringResource(if (app.isFree) R.string.details_free else R.string.details_paid)
    val adsLabel = stringResource(R.string.details_metric_ads)
    val adsValue = stringResource(
        if (app.containsAds) R.string.details_contains_ads else R.string.details_no_ads
    )
    val isUpdateAvailable = state is AppState.Updatable
    val installedVersionName by produceState(
        initialValue = "",
        context.applicationContext,
        app.packageName,
        isUpdateAvailable
    ) {
        value = if (isUpdateAvailable) {
            withContext(Dispatchers.IO) {
                PackageUtil.getInstalledVersionName(context.applicationContext, app.packageName)
            }
        } else {
            ""
        }
    }
    val versionValue = when {
        isUpdateAvailable && installedVersionName.isNotBlank() && app.versionName.isNotBlank() ->
            "$installedVersionName → ${app.versionName}"

        state is AppState.Installed && state.versionName.isNotBlank() -> state.versionName
        else -> app.versionName
    }
    val metrics = remember(
        app.labeledRating,
        app.versionName,
        app.installs,
        app.size,
        updatedOn,
        app.isFree,
        app.containsAds,
        context,
        ratingLabel,
        downloadsLabel,
        sizeLabel,
        updatedLabel,
        priceLabel,
        priceValue,
        adsLabel,
        adsValue,
        versionLabel,
        versionValue
    ) {
        listOfNotNull(
            averageRating?.let { AppMetric(ratingLabel, it, R.drawable.ic_star) },
            versionValue.takeIf { it.isNotBlank() }?.let {
                AppMetric(versionLabel, it, R.drawable.ic_updates)
            },
            CommonUtil.addDiPrefix(app.installs)?.let {
                AppMetric(downloadsLabel, it, R.drawable.ic_download_manager)
            },
            AppMetric(
                sizeLabel,
                Formatter.formatShortFileSize(context, app.size),
                R.drawable.ic_apk_install
            ),
            updatedOn.takeIf { it.isNotBlank() }?.let {
                AppMetric(updatedLabel, it, R.drawable.ic_updates)
            },
            AppMetric(priceLabel, priceValue, R.drawable.ic_paid),
            AppMetric(adsLabel, adsValue, R.drawable.ic_campaign)
        )
    }
    val listState = rememberLazyListState()
    val snapLayoutInfoProvider = remember(listState) {
        SnapLayoutInfoProvider(listState, SnapPosition.Start)
    }
    val decayAnimationSpec = rememberSplineBasedDecay<Float>()
    val snapAnimationSpec = remember {
        spring<Float>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    }
    val flingBehavior = remember(listState, snapLayoutInfoProvider, decayAnimationSpec) {
        snapFlingBehavior(
            snapLayoutInfoProvider = snapLayoutInfoProvider,
            decayAnimationSpec = decayAnimationSpec,
            snapAnimationSpec = snapAnimationSpec
        )
    }

    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 61.dp)
            .clip(RoundedCornerShape(dimensionResource(R.dimen.radius_large))),
        contentPadding = PaddingValues(
            horizontal = dimensionResource(R.dimen.spacing_large),
            vertical = dimensionResource(R.dimen.spacing_xsmall)
        ),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        flingBehavior = flingBehavior
    ) {
        itemsIndexed(
            items = metrics,
            key = { _, metric -> metric.label },
            contentType = { _, _ -> "detail-metric" }
        ) { index, metric ->
            MetricItem(metric = metric, index = index)
        }
    }
}

@Composable
private fun MetricItem(modifier: Modifier = Modifier, metric: AppMetric, index: Int) {
    val (containerColor, contentColor) = when (index % 3) {
        0 -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        1 -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(dimensionResource(R.dimen.radius_large)),
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier
                .padding(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(26.dp),
                shape = RoundedCornerShape(dimensionResource(R.dimen.radius_large)),
                color = contentColor.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(metric.icon),
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = metric.label,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = MaterialTheme.typography.labelLarge.fontSize * 0.8f,
                        lineHeight = MaterialTheme.typography.labelLarge.lineHeight * 0.8f
                    ),
                    color = contentColor.copy(alpha = 0.8f)
                )
                Text(
                    text = metric.value,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = MaterialTheme.typography.titleMedium.fontSize * 0.8f,
                        lineHeight = MaterialTheme.typography.titleMedium.lineHeight * 0.8f
                    )
                )
            }
        }
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun TagsPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
    ) {
        Tags(app = app)
    }
}
