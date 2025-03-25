/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details

import android.text.format.Formatter
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.core.text.HtmlCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImage
import coil3.compose.LocalAsyncImagePreviewHandler
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.aurora.extensions.bodyVerySmall
import com.aurora.extensions.browse
import com.aurora.extensions.copyToClipBoard
import com.aurora.extensions.mailTo
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.Artwork
import com.aurora.store.R
import com.aurora.store.compose.composables.HeaderComposable
import com.aurora.store.compose.composables.InfoComposable
import com.aurora.store.compose.composables.TopAppBarComposable
import com.aurora.store.compose.composables.app.AppProgressComposable
import com.aurora.store.compose.composables.app.AppTagComposable
import com.aurora.store.compose.composables.app.NoAppComposable
import com.aurora.store.compose.composables.details.ScreenshotComposable
import com.aurora.store.compose.composables.preview.AppPreviewProvider
import com.aurora.store.compose.composables.preview.coilPreviewProvider
import com.aurora.store.util.CommonUtil
import com.aurora.store.viewmodel.details.AppDetailsViewModel

@Composable
fun AppDetailsScreen(
    packageName: String,
    onNavigateUp: () -> Unit,
    onNavigateToDetailsMore: () -> Unit,
    onNavigateToDetailsScreenshot: (index: Int) -> Unit,
    onNavigateToDetailsReview: () -> Unit,
    viewModel: AppDetailsViewModel = hiltViewModel()
) {
    val app by viewModel.app.collectAsStateWithLifecycle()

    LaunchedEffect(key1 = Unit) { viewModel.fetchAppDetails(packageName) }

    with(app) {
        when {
            this != null -> {
                if (this.packageName.isBlank()) {
                    ScreenContentLoading(onNavigateUp = onNavigateUp)
                } else {
                    ScreenContent(
                        app = this,
                        onNavigateUp = onNavigateUp,
                        onNavigateToDetailsMore = onNavigateToDetailsMore,
                        onNavigateToDetailsScreenshot = onNavigateToDetailsScreenshot,
                        onNavigateToDetailsReview = onNavigateToDetailsReview
                    )
                }
            }

            // TODO: Deal with different kind of errors
            else -> ScreenContentError(onNavigateUp = onNavigateUp)
        }
    }
}

@Composable
private fun ScreenContentLoading(onNavigateUp: () -> Unit = {}) {
    Scaffold(
        topBar = { TopAppBarComposable(onNavigateUp = onNavigateUp) }
    ) { paddingValues ->
        AppProgressComposable(modifier = Modifier.padding(paddingValues))
    }
}

@Composable
private fun ScreenContentError(onNavigateUp: () -> Unit = {}) {
    Scaffold(
        topBar = { TopAppBarComposable(onNavigateUp = onNavigateUp) }
    ) { paddingValues ->
        NoAppComposable(
            modifier = Modifier.padding(paddingValues),
            icon = R.drawable.ic_apps_outage,
            message = R.string.toast_app_unavailable
        )
    }
}

@Composable
private fun ScreenContent(
    app: App,
    onNavigateUp: () -> Unit = {},
    onNavigateToDetailsMore: () -> Unit = {},
    onNavigateToDetailsScreenshot: (index: Int) -> Unit = {},
    onNavigateToDetailsReview: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBarComposable(
                onNavigateUp = onNavigateUp
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(dimensionResource(R.dimen.padding_medium)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_medium))
        ) {
            // TODO: Deal with download status
            AppDetails(app = app)
            AppActions(
                primaryActionDisplayName = R.string.action_install,
                secondaryActionDisplayName = R.string.title_manual_download
            )

            AppTags(app)
            AppChangelog(changelog = app.changes)
            HeaderComposable(
                title = stringResource(R.string.details_more_about_app),
                subtitle = app.shortDescription,
                onClick = onNavigateToDetailsMore
            )

            AppScreenshots(app.screenshots, onNavigateToDetailsScreenshot)

            HeaderComposable(
                title = stringResource(R.string.details_ratings),
                onClick = onNavigateToDetailsReview
            )

            HeaderComposable(
                title = stringResource(R.string.details_permission),
                subtitle = stringResource(R.string.permissions, app.permissions.size)
            )

            AppDeveloperDetails(
                address = app.developerAddress,
                website = app.developerWebsite,
                email = app.developerEmail
            )
        }
    }
}

/**
 * Composable to display basic app details
 */
@Composable
private fun AppDetails(app: App) {
    Row(modifier = Modifier.fillMaxWidth()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(app.iconArtwork.url)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .requiredSize(dimensionResource(R.dimen.icon_size_large))
                .clip(RoundedCornerShape(dimensionResource(R.dimen.radius_medium)))
        )
        Column(modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.margin_small))) {
            Text(
                text = app.displayName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = app.developerName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodyVerySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.version, app.versionName, app.versionCode),
                style = MaterialTheme.typography.bodyVerySmall
            )
        }
    }
}

/**
 * Composable to display primary and secondary actions available for the app
 */
@Composable
private fun AppActions(
    @StringRes primaryActionDisplayName: Int,
    @StringRes secondaryActionDisplayName: Int,
    isPrimaryActionEnabled: Boolean = true,
    isSecondaryActionEnabled: Boolean = true,
    onPrimaryAction: () -> Unit = {},
    onSecondaryAction: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_medium))
    ) {
        FilledTonalButton(
            modifier = Modifier.weight(1F),
            onClick = onSecondaryAction,
            enabled = isSecondaryActionEnabled
        ) {
            Text(
                text = stringResource(secondaryActionDisplayName),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Button(
            modifier = Modifier.weight(1F),
            onClick = onPrimaryAction,
            enabled = isPrimaryActionEnabled
        ) {
            Text(
                text = stringResource(primaryActionDisplayName),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Composable to display app changelog
 */
@Composable
private fun AppChangelog(changelog: String) {
    HeaderComposable(title = stringResource(R.string.details_changelog))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensionResource(R.dimen.radius_small)))
            .background(color = MaterialTheme.colorScheme.secondaryContainer)
            .padding(dimensionResource(R.dimen.padding_medium))
    ) {
        Text(
            text = if (changelog.isBlank()) {
                stringResource(R.string.details_changelog_unavailable)
            } else {
                HtmlCompat.fromHtml(changelog, HtmlCompat.FROM_HTML_MODE_COMPACT)
                    .toString()
            },
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/**
 * Composable to display tags related to the app
 */
@Composable
private fun AppTags(app: App) {
    val context = LocalContext.current

    val installsLabel = CommonUtil.addDiPrefix(app.installs)
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
        app.labeledRating to R.drawable.ic_star,
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

/**
 * Composable to display details of the app developer
 */
@Composable
private fun AppDeveloperDetails(address: String, website: String, email: String) {
    val context = LocalContext.current

    HeaderComposable(title = stringResource(R.string.details_dev_details))
    Column {
        if (website.isNotBlank()) {
            InfoComposable(
                title = stringResource(R.string.details_dev_website),
                description = website,
                icon = R.drawable.ic_network,
                onClick = { context.browse(website) }
            )
        }

        if (email.isNotBlank()) {
            InfoComposable(
                title = stringResource(R.string.details_dev_email),
                description = email,
                icon = R.drawable.ic_mail,
                onClick = { context.mailTo(email) }
            )
        }

        if (address.isNotBlank()) {
            InfoComposable(
                title = stringResource(R.string.details_dev_address),
                description = HtmlCompat.fromHtml(address, HtmlCompat.FROM_HTML_MODE_COMPACT)
                    .toString(),
                icon = R.drawable.ic_person_location,
                onClick = { context.copyToClipBoard(address) }
            )
        }
    }
}

/**
 * Composable to display screenshots of the app
 */
@Composable
private fun AppScreenshots(list: List<Artwork>, onNavigateToScreenshot: (index: Int) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_small))) {
        items(items = list, key = { artwork -> artwork.url }) { artwork ->
            ScreenshotComposable(
                modifier = Modifier
                    .height(dimensionResource(R.dimen.screenshot_height))
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.radius_small)))
                    .clickable { onNavigateToScreenshot(list.indexOf(artwork)) },
                url = "${artwork.url}=rw-w480-v1-e15"
            )
        }
    }
}

/**
 * Composable to display reviews of the app
 */
@Composable
private fun AppReviews() {

}

@Preview
@Composable
@OptIn(ExperimentalCoilApi::class)
private fun AppDetailsScreenPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    CompositionLocalProvider(LocalAsyncImagePreviewHandler provides coilPreviewProvider) {
        ScreenContent(app = app)
    }
}

@Preview
@Composable
private fun AppDetailsScreenPreviewLoading() {
    ScreenContentLoading()
}

@Preview
@Composable
private fun AppDetailsScreenPreviewError() {
    ScreenContentError()
}
