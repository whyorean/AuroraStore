/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details

import android.content.ActivityNotFoundException
import android.text.format.Formatter
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.util.fastForEach
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
import com.aurora.extensions.toast
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.Artwork
import com.aurora.gplayapi.data.models.Rating
import com.aurora.gplayapi.data.models.datasafety.EntryType
import com.aurora.store.R
import com.aurora.store.compose.composables.HeaderComposable
import com.aurora.store.compose.composables.InfoComposable
import com.aurora.store.compose.composables.TopAppBarComposable
import com.aurora.store.compose.composables.app.AppProgressComposable
import com.aurora.store.compose.composables.app.AppTagComposable
import com.aurora.store.compose.composables.app.NoAppComposable
import com.aurora.store.compose.composables.details.RatingComposable
import com.aurora.store.compose.composables.details.ScreenshotComposable
import com.aurora.store.compose.composables.preview.AppPreviewProvider
import com.aurora.store.compose.composables.preview.coilPreviewProvider
import com.aurora.store.compose.ui.dialogs.ManualDownloadDialog
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.data.model.Report
import com.aurora.store.data.model.Scores
import com.aurora.store.data.room.download.Download
import com.aurora.store.util.CommonUtil
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.PackageUtil.PACKAGE_NAME_GMS
import com.aurora.store.viewmodel.details.AppDetailsViewModel
import java.util.Locale
import com.aurora.gplayapi.data.models.datasafety.Report as DataSafetyReport

@Composable
fun AppDetailsScreen(
    packageName: String,
    onNavigateUp: () -> Unit,
    onNavigateToDetailsMore: () -> Unit,
    onNavigateToDetailsScreenshot: (index: Int) -> Unit,
    onNavigateToDetailsReview: () -> Unit,
    onNavigateToDetailsExodus: () -> Unit,
    viewModel: AppDetailsViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val app by viewModel.app.collectAsStateWithLifecycle()
    val exodusReport by viewModel.exodusReport.collectAsStateWithLifecycle()
    val dataSafetyReport by viewModel.dataSafetyReport.collectAsStateWithLifecycle()
    val plexusScores by viewModel.plexusScores.collectAsStateWithLifecycle()
    val download by viewModel.download.collectAsStateWithLifecycle()

    var shouldShowManualDownloadDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(key1 = Unit) { viewModel.fetchAppDetails(packageName) }
    LaunchedEffect(key1 = Unit) {
        viewModel.purchaseStatus.collect { success ->
            if (success) {
                shouldShowManualDownloadDialog = false
                context.toast(R.string.toast_manual_available)
            } else {
                context.toast(R.string.toast_manual_unavailable)
            }
        }
    }

    if (shouldShowManualDownloadDialog) {
        ManualDownloadDialog(
            currentVersionCode = app!!.versionCode.toLong(),
            onConfirm = { versionCode ->
                viewModel.purchase(app!!.copy(versionCode = versionCode.toInt()))
            },
            onDismiss = { shouldShowManualDownloadDialog = false }
        )
    }

    with(app) {
        when {
            this != null -> {
                if (this.packageName.isBlank()) {
                    ScreenContentLoading(onNavigateUp = onNavigateUp)
                } else {
                    ScreenContent(
                        app = this,
                        download = download,
                        plexusScores = plexusScores,
                        dataSafetyReport = dataSafetyReport,
                        exodusReport = exodusReport,
                        hasValidUpdate = viewModel.hasValidUpdate,
                        showSimilarApps = viewModel.showSimilarApps,
                        onNavigateUp = onNavigateUp,
                        onNavigateToDetailsMore = onNavigateToDetailsMore,
                        onNavigateToDetailsScreenshot = onNavigateToDetailsScreenshot,
                        onNavigateToDetailsReview = onNavigateToDetailsReview,
                        onNavigateToDetailsExodus = onNavigateToDetailsExodus,
                        onDownload = { viewModel.download(this) },
                        onManualDownload = { shouldShowManualDownloadDialog = true },
                        onCancelDownload = { viewModel.cancelDownload(this) },
                        onUninstall = { AppInstaller.uninstall(context, packageName) },
                        onOpen = {
                            try {
                                context.startActivity(
                                    PackageUtil.getLaunchIntent(context, packageName)
                                )
                            } catch (exception: ActivityNotFoundException) {
                                context.toast(context.getString(R.string.unable_to_open))
                            }
                        }
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
    download: Download? = null,
    plexusScores: Scores? = null,
    dataSafetyReport: DataSafetyReport? = null,
    exodusReport: Report? = null,
    hasValidUpdate: Boolean = false,
    showSimilarApps: Boolean = false,
    onNavigateUp: () -> Unit = {},
    onNavigateToDetailsMore: () -> Unit = {},
    onNavigateToDetailsScreenshot: (index: Int) -> Unit = {},
    onNavigateToDetailsReview: () -> Unit = {},
    onNavigateToDetailsExodus: () -> Unit = {},
    onDownload: () -> Unit = {},
    onManualDownload: () -> Unit = {},
    onCancelDownload: () -> Unit = {},
    onUninstall: () -> Unit = {},
    onOpen: () -> Unit = {}
) {
    val context = LocalContext.current

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
            AppDetails(app = app, hasValidUpdate = hasValidUpdate)

            when {
                download?.isRunning == true -> {
                    AppActions(
                        primaryActionDisplayName = stringResource(R.string.action_open),
                        secondaryActionDisplayName = stringResource(R.string.action_cancel),
                        isPrimaryActionEnabled = false,
                        onSecondaryAction = onCancelDownload
                    )
                }

                app.isInstalled && hasValidUpdate -> {
                    AppActions(
                        primaryActionDisplayName = stringResource(R.string.action_update),
                        secondaryActionDisplayName = stringResource(R.string.action_uninstall),
                        onPrimaryAction = onDownload,
                        onSecondaryAction = onUninstall
                    )
                }

                app.isInstalled -> {
                    AppActions(
                        primaryActionDisplayName = stringResource(R.string.action_open),
                        secondaryActionDisplayName = stringResource(R.string.action_uninstall),
                        onPrimaryAction = onOpen,
                        onSecondaryAction = onUninstall
                    )
                }

                else -> {
                    val primaryActionName = if (PackageUtil.isArchived(context, app.packageName)) {
                        stringResource(R.string.action_unarchive)
                    } else {
                        if (app.isFree) stringResource(R.string.action_install) else app.price
                    }

                    AppActions(
                        primaryActionDisplayName = primaryActionName,
                        secondaryActionDisplayName = stringResource(R.string.title_manual_download),
                        onPrimaryAction = onDownload,
                        onSecondaryAction = onManualDownload
                    )
                }
            }

            AppTags(app = app)
            AppChangelog(changelog = app.changes)
            HeaderComposable(
                title = stringResource(R.string.details_more_about_app),
                subtitle = app.shortDescription,
                onClick = onNavigateToDetailsMore
            )

            AppScreenshots(
                list = app.screenshots,
                onNavigateToScreenshot = onNavigateToDetailsScreenshot
            )

            AppReviews(rating = app.rating, onNavigateToDetailsReview = onNavigateToDetailsReview)

            AppCompatibility(
                needsGms = app.dependencies.dependentPackages.contains(PACKAGE_NAME_GMS),
                plexusScores = plexusScores
            )

            HeaderComposable(
                title = stringResource(R.string.details_permission),
                subtitle = stringResource(R.string.permissions, app.permissions.size)
            )

            if (dataSafetyReport != null) AppDataSafety(dataSafetyReport = dataSafetyReport)

            AppPrivacy(
                report = exodusReport,
                onNavigateToDetailsExodus = if (!exodusReport?.trackers.isNullOrEmpty()) {
                    onNavigateToDetailsExodus
                } else {
                    null
                }
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
private fun AppDetails(app: App, hasValidUpdate: Boolean = false) {
    val context = LocalContext.current

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
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = app.developerName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (!hasValidUpdate) {
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

/**
 * Composable to display primary and secondary actions available for the app
 */
@Composable
private fun AppActions(
    primaryActionDisplayName: String,
    secondaryActionDisplayName: String,
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
                text = secondaryActionDisplayName,
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
                text = primaryActionDisplayName,
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
                AnnotatedString(text = stringResource(R.string.details_changelog_unavailable))
            } else {
                AnnotatedString.fromHtml(htmlString = changelog)
            },
            style = MaterialTheme.typography.bodyMedium
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
                title = AnnotatedString(text = stringResource(R.string.details_dev_website)),
                description = AnnotatedString(text = website),
                icon = R.drawable.ic_network,
                onClick = { context.browse(website) }
            )
        }

        if (email.isNotBlank()) {
            InfoComposable(
                title = AnnotatedString(text = stringResource(R.string.details_dev_email)),
                description = AnnotatedString(text = email),
                icon = R.drawable.ic_mail,
                onClick = { context.mailTo(email) }
            )
        }

        if (address.isNotBlank()) {
            InfoComposable(
                title = AnnotatedString(text = stringResource(R.string.details_dev_address)),
                description = AnnotatedString.fromHtml(htmlString = address),
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
private fun AppReviews(rating: Rating, onNavigateToDetailsReview: () -> Unit) {
    val stars = listOf(
        rating.oneStar, rating.twoStar, rating.threeStar, rating.fourStar, rating.fiveStar
    ).map { it.toFloat() }.also {
        // No ratings available, nothing to show
        if (it.sum() == 0F) return
    }

    HeaderComposable(
        title = stringResource(R.string.details_ratings),
        subtitle = stringResource(R.string.details_ratings_subtitle),
        onClick = onNavigateToDetailsReview
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_small))
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.padding_medium)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = String.format(Locale.getDefault(), "%.1f", rating.average),
                maxLines = 1,
                style = MaterialTheme.typography.displayMedium,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = rating.abbreviatedLabel,
                maxLines = 1,
                style = MaterialTheme.typography.bodySmall,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.padding_medium)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_small))
        ) {
            stars.reversed().fastForEach { star ->
                RatingComposable(
                    label = (stars.indexOf(star) + 1).toString(),
                    rating = star / stars.sum()
                )
            }
        }
    }
}

/**
 * Composable to display app compatibility rating from Plexus
 */
@Composable
private fun AppCompatibility(needsGms: Boolean, plexusScores: Scores?) {
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

/**
 * Composable to display app's data safety report
 */
@Composable
private fun AppDataSafety(dataSafetyReport: DataSafetyReport) {
    HeaderComposable(
        title = stringResource(R.string.details_data_safety_title),
        subtitle = stringResource(R.string.details_data_safety_subtitle)
    )

    dataSafetyReport.entries.groupBy { it.type }.forEach { (type, entries) ->
        when (type) {
            EntryType.DATA_COLLECTED -> {
                InfoComposable(
                    icon = R.drawable.ic_cloud_upload,
                    title = AnnotatedString(
                        text = stringResource(R.string.details_data_safety_collect)
                    ),
                    description = AnnotatedString(
                        text = entries.first().subEntries.joinToString(", ") { it.name }
                            .ifBlank {
                                stringResource(R.string.details_data_safety_collect_none)
                            }
                    )
                )
            }

            EntryType.DATA_SHARED -> {
                InfoComposable(
                    icon = R.drawable.ic_share,
                    title = AnnotatedString(
                        text = stringResource(R.string.details_data_safety_shared)
                    ),
                    description = AnnotatedString(
                        text = entries.first().subEntries.joinToString(", ") { it.name }
                            .ifBlank {
                                stringResource(R.string.details_data_safety_share_none)
                            }
                    ),
                )
            }

            // We don't care about any other sections
            else -> {}
        }
    }
}

/**
 * Composable to display app's privacy report from ExodusPrivacy
 */
@Composable
private fun AppPrivacy(report: Report?, onNavigateToDetailsExodus: (() -> Unit)?) {
    HeaderComposable(
        title = stringResource(R.string.details_privacy),
        subtitle = stringResource(R.string.exodus_powered),
        onClick = onNavigateToDetailsExodus
    )

    val reportStatus = when {
        report == null -> stringResource(R.string.failed_to_fetch_report)
        report.id == -1 -> stringResource(R.string.exodus_progress)
        else -> if (report.trackers.isEmpty()) {
            stringResource(R.string.exodus_no_tracker)
        } else {
            stringResource(R.string.exodus_report_trackers, report.trackers.size, report.version)
        }
    }

    InfoComposable(
        icon = R.drawable.ic_visibility,
        title = AnnotatedString(text = reportStatus),
        description = AnnotatedString(text = stringResource(R.string.exodus_tracker_desc))
    )
}

@Preview
@Composable
@OptIn(ExperimentalCoilApi::class)
private fun AppDetailsScreenPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    CompositionLocalProvider(LocalAsyncImagePreviewHandler provides coilPreviewProvider) {
        ScreenContent(app = app, hasValidUpdate = false)
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
