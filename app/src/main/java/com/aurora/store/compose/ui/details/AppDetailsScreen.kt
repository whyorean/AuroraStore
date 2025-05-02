/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details

import android.content.ActivityNotFoundException
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableSupportingPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberSupportingPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.LocalAsyncImagePreviewHandler
import com.aurora.extensions.toast
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.compose.composables.HeaderComposable
import com.aurora.store.compose.composables.TopAppBarComposable
import com.aurora.store.compose.composables.app.AppProgressComposable
import com.aurora.store.compose.composables.app.NoAppComposable
import com.aurora.store.compose.composables.preview.AppPreviewProvider
import com.aurora.store.compose.composables.preview.coilPreviewProvider
import com.aurora.store.compose.navigation.Screen
import com.aurora.store.compose.ui.details.components.AppActions
import com.aurora.store.compose.ui.details.components.AppChangelog
import com.aurora.store.compose.ui.details.components.AppCompatibility
import com.aurora.store.compose.ui.details.components.AppDataSafety
import com.aurora.store.compose.ui.details.components.AppDetails
import com.aurora.store.compose.ui.details.components.AppDeveloperDetails
import com.aurora.store.compose.ui.details.components.AppPrivacy
import com.aurora.store.compose.ui.details.components.AppReviews
import com.aurora.store.compose.ui.details.components.AppScreenshots
import com.aurora.store.compose.ui.details.components.AppTags
import com.aurora.store.compose.ui.details.components.AppTesting
import com.aurora.store.compose.ui.dev.DevProfileScreen
import com.aurora.store.compose.ui.dialogs.ManualDownloadDialog
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.data.model.Report
import com.aurora.store.data.model.Scores
import com.aurora.store.data.room.download.Download
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.PackageUtil.PACKAGE_NAME_GMS
import com.aurora.store.viewmodel.details.AppDetailsViewModel
import kotlinx.coroutines.launch
import com.aurora.gplayapi.data.models.datasafety.Report as DataSafetyReport

@Composable
fun AppDetailsScreen(
    packageName: String,
    onNavigateUp: () -> Unit,
    onNavigateToAppDetails: (packageName: String) -> Unit,
    viewModel: AppDetailsViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val app by viewModel.app.collectAsStateWithLifecycle()
    val exodusReport by viewModel.exodusReport.collectAsStateWithLifecycle()
    val dataSafetyReport by viewModel.dataSafetyReport.collectAsStateWithLifecycle()
    val plexusScores by viewModel.plexusScores.collectAsStateWithLifecycle()
    val download by viewModel.download.collectAsStateWithLifecycle()
    val installProgress by viewModel.installProgress.collectAsStateWithLifecycle()

    var shouldShowManualDownloadDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(key1 = Unit) { viewModel.fetchAppDetails(packageName) }
    LaunchedEffect(key1 = Unit) {
        viewModel.purchaseStatus.collect { success ->
            if (shouldShowManualDownloadDialog) {
                if (success) {
                    shouldShowManualDownloadDialog = false
                    context.toast(R.string.toast_manual_available)
                } else {
                    context.toast(R.string.toast_manual_unavailable)
                }
            } else {
                context.toast(R.string.download_failed)
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
                    ScreenContentApp(
                        app = this,
                        isAnonymous = viewModel.authProvider.isAnonymous,
                        download = download,
                        installProgress = installProgress,
                        plexusScores = plexusScores,
                        dataSafetyReport = dataSafetyReport,
                        exodusReport = exodusReport,
                        hasValidUpdate = viewModel.hasValidUpdate,
                        onNavigateUp = onNavigateUp,
                        onNavigateToAppDetails = onNavigateToAppDetails,
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
                        },
                        onTestingSubscriptionChange = { subscribe ->
                            viewModel.updateTestingProgramStatus(packageName, subscribe)
                        }
                    )
                }
            }

            // TODO: Deal with different kind of errors
            else -> ScreenContentError(onNavigateUp = onNavigateUp)
        }
    }
}

/**
 * Composable to show progress while fetching app details
 */
@Composable
private fun ScreenContentLoading(onNavigateUp: () -> Unit = {}) {
    Scaffold(
        topBar = { TopAppBarComposable(onNavigateUp = onNavigateUp) }
    ) { paddingValues ->
        AppProgressComposable(modifier = Modifier.padding(paddingValues))
    }
}

/**
 * Composable to display errors related to fetching app details
 */
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

/**
 * Composable to display app details and suggestions
 */
@Composable
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private fun ScreenContentApp(
    app: App,
    isAnonymous: Boolean = true,
    download: Download? = null,
    installProgress: Float? = null,
    plexusScores: Scores? = null,
    dataSafetyReport: com.aurora.gplayapi.data.models.datasafety.Report? = null,
    exodusReport: Report? = null,
    hasValidUpdate: Boolean = false,
    onNavigateUp: () -> Unit = {},
    onNavigateToAppDetails: (packageName: String) -> Unit = {},
    onDownload: () -> Unit = {},
    onManualDownload: () -> Unit = {},
    onCancelDownload: () -> Unit = {},
    onUninstall: () -> Unit = {},
    onOpen: () -> Unit = {},
    onTestingSubscriptionChange: (subscribe: Boolean) -> Unit = {}
) {
    val scaffoldNavigator = rememberSupportingPaneScaffoldNavigator<Screen>()
    val coroutineScope = rememberCoroutineScope()

    fun showMainPane() {
        coroutineScope.launch {
            scaffoldNavigator.navigateBack()
        }
    }

    fun showExtraPane(screen: Screen) {
        coroutineScope.launch {
            scaffoldNavigator.navigateTo(SupportingPaneScaffoldRole.Extra, screen)
        }
    }

    NavigableSupportingPaneScaffold(
        navigator = scaffoldNavigator,
        mainPane = {
            AnimatedPane {
                ScreenContentAppMainPane(
                    app = app,
                    isAnonymous = isAnonymous,
                    download = download,
                    installProgress = installProgress,
                    plexusScores = plexusScores,
                    dataSafetyReport = dataSafetyReport,
                    exodusReport = exodusReport,
                    hasValidUpdate = hasValidUpdate,
                    onNavigateUp = onNavigateUp,
                    onDownload = onDownload,
                    onManualDownload = onManualDownload,
                    onCancelDownload = onCancelDownload,
                    onUninstall = onUninstall,
                    onOpen = onOpen,
                    onNavigateToDetailsDevProfile = { showExtraPane(Screen.DevProfile(it)) },
                    onNavigateToDetailsMore = { showExtraPane(Screen.DetailsMore) },
                    onNavigateToDetailsReview = { showExtraPane(Screen.DetailsReview) },
                    onNavigateToDetailsExodus = { showExtraPane(Screen.DetailsExodus) },
                    onNavigateToDetailsScreenshot = { showExtraPane(Screen.DetailsScreenshot(it)) },
                    onTestingSubscriptionChange = onTestingSubscriptionChange
                )
            }
        },
        supportingPane = {
            AnimatedPane {
                DetailsSuggestions(
                    onNavigateUp = null,
                    onNavigateToAppDetails = onNavigateToAppDetails
                )
            }
        },
        extraPane = {
            scaffoldNavigator.currentDestination?.contentKey?.let { screen ->
                AnimatedPane {
                    when (screen) {
                        is Screen.DetailsReview -> DetailsReviewScreen(onNavigateUp = ::showMainPane)
                        is Screen.DetailsExodus -> DetailsExodusScreen(onNavigateUp = ::showMainPane)
                        is Screen.DetailsMore -> DetailsMoreScreen(
                            onNavigateUp = ::showMainPane,
                            onNavigateToAppDetails = onNavigateToAppDetails
                        )

                        is Screen.DetailsScreenshot -> DetailsScreenshotScreen(
                            index = screen.index,
                            onNavigateUp = ::showMainPane
                        )
                        // TODO: Pass the real developerId
                        is Screen.DevProfile -> DevProfileScreen(
                            developerId = app.developerName,
                            onNavigateUp = ::showMainPane,
                            onNavigateToAppDetails = { onNavigateToAppDetails(it) }
                        )

                        else -> {}
                    }
                }
            }
        }
    )
}

/**
 * Composable to display app details
 */
@Composable
private fun ScreenContentAppMainPane(
    app: App,
    download: Download?,
    installProgress: Float?,
    isAnonymous: Boolean,
    plexusScores: Scores?,
    dataSafetyReport: DataSafetyReport?,
    exodusReport: Report?,
    hasValidUpdate: Boolean,
    onNavigateUp: () -> Unit,
    onNavigateToDetailsDevProfile: (developerName: String) -> Unit,
    onNavigateToDetailsMore: () -> Unit,
    onNavigateToDetailsScreenshot: (index: Int) -> Unit,
    onNavigateToDetailsReview: () -> Unit,
    onNavigateToDetailsExodus: () -> Unit,
    onDownload: () -> Unit,
    onManualDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onUninstall: () -> Unit,
    onOpen: () -> Unit,
    onTestingSubscriptionChange: (subscribe: Boolean) -> Unit
) {
    val context = LocalContext.current

    @Composable
    fun SetupAppActions() {
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
    }

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
            val isDownloading = download != null && download.isRunning
            val isInstalling = installProgress != null
            val progress = when {
                isDownloading -> download?.progress?.toFloat() ?: 0F
                isInstalling -> installProgress ?: 0F
                else -> 0F
            }

            AppDetails(
                app = app,
                inProgress = isDownloading || isInstalling,
                progress = progress,
                onNavigateToDetailsDevProfile = onNavigateToDetailsDevProfile,
                hasValidUpdate = hasValidUpdate
            )

            SetupAppActions()

            AppTags(app = app)
            AppChangelog(changelog = app.changes)
            HeaderComposable(
                title = stringResource(R.string.details_more_about_app),
                subtitle = app.shortDescription,
                onClick = onNavigateToDetailsMore
            )

            AppScreenshots(
                screenshots = app.screenshots,
                onNavigateToScreenshot = onNavigateToDetailsScreenshot
            )

            AppReviews(rating = app.rating, onNavigateToDetailsReview = onNavigateToDetailsReview)

            if (!isAnonymous && app.testingProgram?.isAvailable == true) {
                AppTesting(
                    isSubscribed = app.testingProgram!!.isSubscribed,
                    onTestingSubscriptionChange = onTestingSubscriptionChange
                )
            }

            AppCompatibility(
                needsGms = app.dependencies.dependentPackages.contains(PACKAGE_NAME_GMS),
                plexusScores = plexusScores
            )

            HeaderComposable(
                title = stringResource(R.string.details_permission),
                subtitle = stringResource(R.string.permissions, app.permissions.size)
            )

            if (dataSafetyReport != null) AppDataSafety(report = dataSafetyReport)

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

@PreviewScreenSizes
@Composable
@OptIn(ExperimentalCoilApi::class)
private fun AppDetailsScreenPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    CompositionLocalProvider(LocalAsyncImagePreviewHandler provides coilPreviewProvider) {
        ScreenContentApp(
            app = app,
            isAnonymous = false,
            hasValidUpdate = false
        )
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
