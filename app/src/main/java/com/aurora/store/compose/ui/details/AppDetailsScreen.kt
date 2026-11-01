/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.AdaptStrategy
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldDefaults
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.NavigableSupportingPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberSupportingPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.aurora.Constants
import com.aurora.extensions.appInfo
import com.aurora.extensions.requiresGMS
import com.aurora.extensions.requiresObbDir
import com.aurora.extensions.share
import com.aurora.extensions.toast
import com.aurora.gplayapi.model.App
import com.aurora.gplayapi.model.DataSafetyReport
import com.aurora.gplayapi.model.Review
import com.aurora.gplayapi.model.StreamBundle
import com.aurora.gplayapi.model.StreamCluster
import com.aurora.gplayapi.model.TopChart
import com.aurora.store.ComposeActivity
import com.aurora.store.R
import com.aurora.store.compose.composable.ClusterRow
import com.aurora.store.compose.composable.ContainedLoadingIndicator
import com.aurora.store.compose.composable.InsufficientStorageDialog
import com.aurora.store.compose.composable.PermissionUpdateWarningDialog
import com.aurora.store.compose.composable.Placeholder
import com.aurora.store.compose.composable.ScrollHint
import com.aurora.store.compose.composable.SectionHeader
import com.aurora.store.compose.composable.ShimmerCarouselSection
import com.aurora.store.compose.composable.StreamCarousel
import com.aurora.store.compose.composable.TopAppBar
import com.aurora.store.compose.composable.TrackerUpdateWarningDialog
import com.aurora.store.compose.navigation.Destination
import com.aurora.store.compose.navigation.Screen
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.ThemePreviewProvider
import com.aurora.store.compose.ui.commons.ForceRestartDialog
import com.aurora.store.compose.ui.commons.PermissionRationaleScreen
import com.aurora.store.compose.ui.details.composable.Actions
import com.aurora.store.compose.ui.details.composable.Changelog
import com.aurora.store.compose.ui.details.composable.Compatibility
import com.aurora.store.compose.ui.details.composable.DataSafety
import com.aurora.store.compose.ui.details.composable.Details
import com.aurora.store.compose.ui.details.composable.DeveloperDetails
import com.aurora.store.compose.ui.details.composable.PlayListing
import com.aurora.store.compose.ui.details.composable.Privacy
import com.aurora.store.compose.ui.details.composable.RatingAndReviews
import com.aurora.store.compose.ui.details.composable.Screenshots
import com.aurora.store.compose.ui.details.composable.Tags
import com.aurora.store.compose.ui.details.composable.Testing
import com.aurora.store.compose.ui.details.composable.UserReview
import com.aurora.store.compose.ui.details.menu.AppDetailsMenu
import com.aurora.store.compose.ui.details.menu.MenuItem
import com.aurora.store.compose.ui.details.navigation.ExtraScreen
import com.aurora.store.compose.ui.dev.DevProfileScreen
import com.aurora.store.compose.ui.sheets.AccountPickerSheet
import com.aurora.store.compose.ui.sheets.InstallErrorSheet
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.data.model.AppState
import com.aurora.store.data.model.ExodusTracker
import com.aurora.store.data.model.PermissionType
import com.aurora.store.data.model.Report
import com.aurora.store.data.model.Scores
import com.aurora.store.data.model.StorageRequirement
import com.aurora.store.data.providers.PermissionProvider.Companion.isGranted
import com.aurora.store.data.providers.PermissionProvider.Companion.isPermittedToInstall
import com.aurora.store.data.room.account.Account
import com.aurora.store.util.FlavouredUtil
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_WARN_PERMISSIONS
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_WARN_TRACKERS
import com.aurora.store.util.ShortcutManagerUtil
import com.aurora.store.util.StorageUtil
import com.aurora.store.viewmodel.details.AppDetailsViewModel
import com.jakewharton.processphoenix.ProcessPhoenix
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun AppDetailsScreen(
    packageName: String,
    onNavigateTo: (Destination) -> Unit,
    viewModel: AppDetailsViewModel = hiltViewModel(key = packageName),
    forceSinglePane: Boolean = false
) {
    val context = LocalContext.current

    val app by viewModel.app.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val featuredReviews by viewModel.featuredReviews.collectAsStateWithLifecycle()
    val userReview by viewModel.userReview.collectAsStateWithLifecycle()
    val favorite by viewModel.favourite.collectAsStateWithLifecycle()
    val exodusReport by viewModel.exodusReport.collectAsStateWithLifecycle()
    val dataSafetyReport by viewModel.dataSafetyReport.collectAsStateWithLifecycle()
    val categoryIconUrl by viewModel.categoryIconUrl.collectAsStateWithLifecycle()
    val plexusScores by viewModel.plexusScores.collectAsStateWithLifecycle()
    val installError by viewModel.installError.collectAsStateWithLifecycle()
    val suggestionsBundle by viewModel.suggestionsBundle.collectAsStateWithLifecycle()
    val canDownload by viewModel.canDownload.collectAsStateWithLifecycle()
    val testingProgramUpdating by viewModel.testingProgramUpdating.collectAsStateWithLifecycle()
    val testingProgramSubscribed by
        viewModel.testingProgramSubscribed.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    var storageWarning by remember { mutableStateOf<StorageRequirement?>(null) }

    LaunchedEffect(key1 = packageName) { viewModel.fetchAppDetails(packageName) }

    LaunchedEffect(Unit) {
        viewModel.storageWarning.collect { storageWarning = it }
    }

    LaunchedEffect(Unit) {
        viewModel.reviewPosted.collect { success ->
            context.toast(
                if (success) R.string.toast_rated_success else R.string.toast_rated_failed
            )
        }
    }

    app?.let { loadedApp ->
        installError?.let { err ->
            InstallErrorSheet(
                app = loadedApp,
                error = err.error,
                extra = err.extra,
                isAnonymous = viewModel.authProvider.isAnonymous,
                onBuy = { openPlayStore(context, loadedApp.packageName) },
                onDismiss = viewModel::dismissInstallError
            )
        }
    }

    AnimatedContent(
        targetState = state,
        contentKey = { stateKey(it, app) },
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "AppDetailsState"
    ) { currentState ->
        when {
            currentState is AppState.Error ->
                ScreenContentError(
                    message = currentState.message,
                    onRetry = { viewModel.fetchAppDetails(packageName) }
                )

            currentState is AppState.Loading || app == null ->
                ScreenContentLoading()

            else -> {
                val loadedApp = app!!
                ScreenContentApp(
                    app = loadedApp,
                    featuredReviews = featuredReviews,
                    userReview = userReview,
                    suggestionsBundle = suggestionsBundle,
                    isFavorite = favorite,
                    isAnonymous = viewModel.authProvider.isAnonymous,
                    canDownload = canDownload,
                    testingProgramUpdating = testingProgramUpdating,
                    testingProgramSubscribed = testingProgramSubscribed,
                    state = currentState,
                    plexusScores = plexusScores,
                    dataSafetyReport = dataSafetyReport,
                    categoryIconUrl = categoryIconUrl,
                    exodusReport = exodusReport,
                    onNavigateTo = onNavigateTo,
                    onLoadMoreCluster = { cluster -> viewModel.loadMoreCluster(cluster) },
                    accounts = accounts,
                    storageWarning = storageWarning,
                    onDismissStorageWarning = { storageWarning = null },
                    onDownload = { requestedApp -> viewModel.enqueueDownload(requestedApp) },
                    onDownloadWith = { requestedApp, accountId ->
                        viewModel.enqueueDownloadWith(requestedApp, accountId)
                    },
                    onFavorite = { viewModel.toggleFavourite(loadedApp) },
                    onCancelDownload = { viewModel.cancelDownload(loadedApp) },
                    onUninstall = { AppInstaller.uninstall(context, packageName) },
                    onOpen = {
                        try {
                            context.startActivity(
                                PackageUtil.getLaunchIntent(context, packageName)
                            )
                        } catch (_: ActivityNotFoundException) {
                            context.toast(R.string.unable_to_open)
                        }
                    },
                    onTestingSubscriptionChange = { subscribe ->
                        viewModel.updateTestingProgramStatus(packageName, subscribe)
                    },
                    onSubmitReview = { rating, title, comment ->
                        viewModel.postAppReview(
                            loadedApp.packageName,
                            Review(title = title, comment = comment, rating = rating),
                            testingProgramSubscribed
                                ?: loadedApp.testingProgram?.isSubscribed
                                ?: false
                        )
                    },
                    onDeleteReview = { viewModel.deleteAppReview(loadedApp) },
                    forceSinglePane = forceSinglePane,
                    onCheckNewTrackers = { pkg, installedVc ->
                        viewModel.getNewTrackers(pkg, installedVc)
                    },
                    onFetchTopChart = viewModel::topChartCluster,
                    onForceRestart = {
                        val intent = Intent(context, ComposeActivity::class.java)
                            .putExtra("packageName", packageName)
                        ProcessPhoenix.triggerRebirth(context, intent)
                    }
                )
            }
        }
    }
}

/**
 * Opens the given app's Play Store listing, preferring the Play Store app when available and
 * falling back to whichever activity can handle the web listing (e.g. a browser).
 */
private fun openPlayStore(context: Context, packageName: String) {
    val uri = "${Constants.SHARE_URL}$packageName".toUri()
    val intent = Intent(Intent.ACTION_VIEW).apply { data = uri }

    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent.apply { setPackage(Constants.PACKAGE_NAME_PLAY_STORE) })
    } else {
        context.startActivity(intent)
    }
}

private fun stateKey(state: AppState, app: App?): String = when {
    state is AppState.Error -> "error"
    state is AppState.Loading || app == null -> "loading"
    else -> "content"
}

/**
 * Composable to show progress while fetching app details
 */
@Composable
private fun ScreenContentLoading() {
    Scaffold(
        topBar = { TopAppBar() }
    ) { paddingValues ->
        ContainedLoadingIndicator(modifier = Modifier.padding(paddingValues))
    }
}

/**
 * Composable to display errors related to fetching app details
 */
@Composable
private fun ScreenContentError(message: String? = null, onRetry: (() -> Unit)? = null) {
    Scaffold(
        topBar = { TopAppBar() }
    ) { paddingValues ->
        Placeholder(
            modifier = Modifier.padding(paddingValues),
            painter = painterResource(R.drawable.ic_refresh),
            message = message ?: stringResource(R.string.toast_app_unavailable),
            actionLabel = onRetry?.let { stringResource(R.string.action_retry) },
            onAction = onRetry
        )
    }
}

/**
 * Composable to display app details and suggestions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScreenContentApp(
    app: App,
    featuredReviews: List<Review> = emptyList(),
    userReview: Review? = null,
    suggestionsBundle: StreamBundle? = StreamBundle.EMPTY,
    isFavorite: Boolean = false,
    isAnonymous: Boolean = true,
    canDownload: Boolean = false,
    testingProgramUpdating: Boolean = false,
    testingProgramSubscribed: Boolean? = null,
    state: AppState = AppState.Unavailable,
    plexusScores: Scores? = null,
    dataSafetyReport: DataSafetyReport? = null,
    categoryIconUrl: String? = null,
    exodusReport: Report? = null,
    onNavigateTo: (Destination) -> Unit = {},
    onLoadMoreCluster: (cluster: StreamCluster) -> Unit = {},
    accounts: List<Account> = emptyList(),
    storageWarning: StorageRequirement? = null,
    onDismissStorageWarning: () -> Unit = {},
    onDownload: (requestedApp: App) -> Unit = {},
    onDownloadWith: (requestedApp: App, accountId: String) -> Unit = { _, _ -> },
    onFavorite: () -> Unit = {},
    onCancelDownload: () -> Unit = {},
    onUninstall: () -> Unit = {},
    onOpen: () -> Unit = {},
    onTestingSubscriptionChange: (subscribe: Boolean) -> Unit = {},
    onSubmitReview: (rating: Int, title: String, comment: String) -> Unit = { _, _, _ -> },
    onDeleteReview: () -> Unit = {},
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfoV2(),
    forceSinglePane: Boolean = false,
    onFetchTopChart: suspend (chart: TopChart, title: String) -> StreamCluster? = { _, _ -> null },
    onCheckNewTrackers: suspend (
        packageName: String,
        installedVersionCode: Long
    ) -> List<ExodusTracker> =
        { _, _ -> emptyList() },
    onForceRestart: () -> Unit = {}
) {
    val context = LocalContext.current

    // Gated decision 1 (phase-6 migration plan): gate on the session's DOWNLOAD capability
    // rather than on account presence. Aurora's isAnonymous ("dispensed Play account") is a
    // different concept from a gplayapi-v4 anonymous session, and every anonymously-fetched
    // app's isFree is false (the V2 proto's Price is always empty), so gating on !isAnonymous
    // the way v3 did would make Install vanish catalogue-wide for anonymous sessions. Free
    // apps are always acquirable regardless.
    val canAcquire = app.isFree || canDownload

    var scaffoldDirective = calculatePaneScaffoldDirective(windowAdaptiveInfo)
    var isScreenshotOpen by remember { mutableStateOf(false) }

    if (forceSinglePane || isScreenshotOpen) {
        scaffoldDirective = scaffoldDirective.copy(maxHorizontalPartitions = 1)
    }

    val scaffoldNavigator = rememberSupportingPaneScaffoldNavigator<NavKey>(
        scaffoldDirective = scaffoldDirective,
        adaptStrategies = SupportingPaneScaffoldDefaults.adaptStrategies(
            supportingPaneAdaptStrategy = AdaptStrategy.Hide
        )
    )
    val showsScreenshot =
        scaffoldNavigator.currentDestination?.contentKey is ExtraScreen.Screenshot
    LaunchedEffect(showsScreenshot) { if (!showsScreenshot) isScreenshotOpen = false }
    val coroutineScope = rememberCoroutineScope()

    fun onTopChartClick(chart: TopChart, title: String) {
        coroutineScope.launch {
            val cluster = onFetchTopChart(chart, title)
            if (cluster != null) {
                onNavigateTo(Destination.StreamBrowse(cluster))
            } else {
                context.toast(R.string.no_apps_available)
            }
        }
    }
    val shouldShowMenuOnMainPane = scaffoldNavigator
        .scaffoldValue[SupportingPaneScaffoldRole.Supporting] == PaneAdaptedValue.Hidden
    var showRestartDialog by remember { mutableStateOf(false) }
    var showAccountPicker by remember { mutableStateOf(false) }
    val warnTrackers = remember {
        Preferences.getBoolean(context, PREFERENCE_UPDATES_WARN_TRACKERS, false)
    }
    var trackerWarning by remember { mutableStateOf<List<ExodusTracker>?>(null) }
    val warnPermissions = remember {
        Preferences.getBoolean(context, PREFERENCE_UPDATES_WARN_PERMISSIONS, false)
    }
    var permissionWarning by remember { mutableStateOf<List<String>?>(null) }
    var isChecking by remember { mutableStateOf(false) }
    var checkJob by remember { mutableStateOf<Job?>(null) }

    // Keep the "checking" actions until the app leaves Updatable (i.e. the download it started has
    // taken over the state), so the button doesn't flash back to "Update" between the two.
    LaunchedEffect(state) {
        if (state !is AppState.Updatable) isChecking = false
    }

    // A blocked download leaves the app Updatable, so the effect above never fires.
    LaunchedEffect(storageWarning) {
        if (storageWarning != null) isChecking = false
    }

    if (showRestartDialog) {
        ForceRestartDialog(onConfirm = onForceRestart)
    }

    fun onNavigateBack() {
        coroutineScope.launch {
            scaffoldNavigator.navigateBack()
        }
    }

    fun showExtraPane(screen: NavKey) {
        coroutineScope.launch {
            if (screen is ExtraScreen.Screenshot) {
                isScreenshotOpen = true
                // The scaffold only re-lays out on navigation; let the directive land first
                withFrameNanos { }
                withFrameNanos { }
            }
            scaffoldNavigator.navigateTo(SupportingPaneScaffoldRole.Extra, screen)
        }
    }

    fun onInstall(
        requestedApp: App = app,
        ignoreMicroG: Boolean = false,
        accountId: String? = null
    ) {
        if (isPermittedToInstall(context, app)) {
            val shouldPromptMicroGInstall = app.requiresGMS() &&
                FlavouredUtil.promptMicroGInstall(context)

            if (shouldPromptMicroGInstall && !ignoreMicroG) {
                isChecking = false
                showExtraPane(ExtraScreen.MicroG)
            } else {
                if (accountId != null) {
                    onDownloadWith(requestedApp, accountId)
                } else {
                    onDownload(requestedApp)
                }
                onNavigateBack()
            }
        } else {
            isChecking = false
            val requiredPermissions = setOfNotNull(
                PermissionType.INSTALL_UNKNOWN_APPS,
                if (app.fileList.requiresObbDir()) PermissionType.STORAGE_MANAGER else null,
                if (app.fileList.requiresObbDir()) PermissionType.EXTERNAL_STORAGE else null
            )
            showExtraPane(Screen.PermissionRationale(requiredPermissions = requiredPermissions))
        }
    }

    fun onCancelCheck() {
        checkJob?.cancel()
        checkJob = null
        isChecking = false
    }

    fun checkTrackersThenInstall() {
        if (!warnTrackers) {
            onInstall()
            return
        }
        isChecking = true
        checkJob = coroutineScope.launch {
            val installedVc =
                PackageUtil.getInstalledVersionCode(context, app.packageName)
            val trackers = onCheckNewTrackers(app.packageName, installedVc)
            if (trackers.isEmpty()) {
                onInstall()
            } else {
                trackerWarning = trackers
            }
        }
    }

    fun onUpdateClicked() {
        val newPermissions = if (warnPermissions) {
            PackageUtil.getNewPermissions(context, app.packageName, app.permissions)
        } else {
            emptyList()
        }
        if (newPermissions.isEmpty()) {
            checkTrackersThenInstall()
        } else {
            permissionWarning = newPermissions
        }
    }

    permissionWarning?.let { permissions ->
        PermissionUpdateWarningDialog(
            permissions = permissions,
            onConfirm = {
                permissionWarning = null
                checkTrackersThenInstall()
            },
            onDismiss = { permissionWarning = null }
        )
    }

    trackerWarning?.let { trackers ->
        TrackerUpdateWarningDialog(
            trackers = trackers,
            onConfirm = {
                trackerWarning = null
                onInstall()
            },
            onDismiss = {
                trackerWarning = null
                isChecking = false
            }
        )
    }

    storageWarning?.let { requirement ->
        InsufficientStorageDialog(
            requirement = requirement,
            onFreeUpSpace = {
                onDismissStorageWarning()
                StorageUtil.openFreeUpSpace(context)
            },
            onDismiss = onDismissStorageWarning
        )
    }

    if (showAccountPicker) {
        AccountPickerSheet(
            accounts = accounts,
            onSelect = { account ->
                showAccountPicker = false
                onInstall(accountId = account.id)
            },
            onDismiss = { showAccountPicker = false }
        )
    }

    @Composable
    fun SetupMenu() {
        AppDetailsMenu(
            isFavorite = isFavorite,
            state = state,
            canManualDownload = canAcquire,
            canUseOtherAccount = accounts.size > 1
        ) { menuItem ->
            when (menuItem) {
                MenuItem.FAVORITE -> onFavorite()

                MenuItem.MANUAL_DOWNLOAD -> {
                    showExtraPane(ExtraScreen.ManualDownload)
                }

                MenuItem.INSTALL_OTHER_ACCOUNT -> {
                    showAccountPicker = true
                }

                MenuItem.SHARE -> context.share(app.displayName, app.packageName)

                MenuItem.APP_INFO -> context.appInfo(app.packageName)

                MenuItem.ADD_TO_HOME -> {
                    ShortcutManagerUtil.requestPinShortcut(context, app.packageName)
                }

                MenuItem.PLAY_STORE -> openPlayStore(context, app.packageName)
            }
        }
    }

    @Composable
    fun SetupActions() {
        if (isChecking) {
            Actions(
                primaryActionDisplayName = stringResource(R.string.action_open),
                secondaryActionDisplayName = stringResource(R.string.action_cancel),
                isPrimaryActionEnabled = false,
                onSecondaryAction = ::onCancelCheck
            )
            return
        }
        AnimatedContent(
            targetState = state,
            contentKey = { it::class },
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "Actions"
        ) { currentState ->
            when (currentState) {
                is AppState.Queued,
                is AppState.Purchasing,
                is AppState.Downloading -> {
                    Actions(
                        primaryActionDisplayName = stringResource(R.string.action_open),
                        secondaryActionDisplayName = stringResource(R.string.action_cancel),
                        isPrimaryActionEnabled = false,
                        onSecondaryAction = onCancelDownload
                    )
                }

                is AppState.Verifying,
                is AppState.Installing -> {
                    Actions(
                        primaryActionDisplayName = stringResource(R.string.action_open),
                        secondaryActionDisplayName = stringResource(R.string.action_cancel),
                        isPrimaryActionEnabled = false,
                        onSecondaryAction = onCancelDownload
                    )
                }

                is AppState.Updatable -> {
                    Actions(
                        primaryActionDisplayName = stringResource(R.string.action_update),
                        secondaryActionDisplayName = stringResource(R.string.action_uninstall),
                        onPrimaryAction = ::onUpdateClicked,
                        onSecondaryAction = onUninstall
                    )
                }

                is AppState.Installed -> {
                    val canOpen = remember(app.packageName) {
                        PackageUtil.getLaunchIntent(context, app.packageName) != null
                    }
                    Actions(
                        primaryActionDisplayName = stringResource(R.string.action_open),
                        secondaryActionDisplayName = stringResource(R.string.action_uninstall),
                        onPrimaryAction = onOpen,
                        onSecondaryAction = onUninstall,
                        isPrimaryActionEnabled = canOpen
                    )
                }

                else -> {
                    val primaryActionName = if (currentState is AppState.Archived) {
                        stringResource(R.string.action_unarchive)
                    } else {
                        if (app.isFree) stringResource(R.string.action_install) else app.price
                    }

                    Actions(
                        primaryActionDisplayName = primaryActionName,
                        secondaryActionDisplayName = stringResource(
                            R.string.title_manual_download
                        ),
                        isPrimaryActionEnabled = canAcquire,
                        isSecondaryActionEnabled = canAcquire,
                        onPrimaryAction = ::onInstall,
                        onSecondaryAction = { showExtraPane(ExtraScreen.ManualDownload) }
                    )
                }
            }
        }
    }

    @Composable
    fun MainPane() {
        Scaffold(
            topBar = {
                TopAppBar(
                    actions = { if (shouldShowMenuOnMainPane) SetupMenu() }
                )
            }
        ) { paddingValues ->
            val listState = rememberLazyListState()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(
                        dimensionResource(R.dimen.spacing_medium)
                    ),
                    state = listState
                ) {
                    item {
                        Details(
                            app = app,
                            state = state,
                            onNavigateToDetailsDevProfile = { showExtraPane(Screen.DevProfile(it)) }
                        )
                    }

                    item {
                        SetupActions()
                    }

                    item {
                        Tags(app = app)
                    }

                    item {
                        PlayListing(
                            app = app,
                            categoryIconUrl = categoryIconUrl,
                            onCategoryClick = { onNavigateTo(Destination.CategoryBrowse(it)) },
                            onTopChartClick = ::onTopChartClick,
                            onBrowse = { title, browseUrl ->
                                onNavigateTo(Destination.ExpandedStreamBrowse(title, browseUrl))
                            }
                        )
                    }

                    item {
                        Changelog(changelog = app.changes)
                    }

                    item {
                        SectionHeader(
                            title = stringResource(R.string.details_more_about_app),
                            subtitle = app.shortDescription,
                            onClick = { showExtraPane(ExtraScreen.More) }
                        )
                    }

                    item {
                        Screenshots(
                            screenshots = app.screenshots,
                            onNavigateToScreenshot = { showExtraPane(ExtraScreen.Screenshot(it)) }
                        )
                    }

                    item {
                        RatingAndReviews(
                            rating = app.rating,
                            featuredReviews = featuredReviews,
                            onNavigateToDetailsReview = { showExtraPane(ExtraScreen.Review) }
                        )
                    }

                    item {
                        // Reviews can only be submitted by personal accounts for installed apps.
                        if (!isAnonymous && app.isInstalled) {
                            UserReview(
                                review = userReview,
                                onSubmit = onSubmitReview,
                                onDelete = onDeleteReview
                            )
                        }
                    }

                    item {
                        if (!isAnonymous && app.testingProgram?.isAvailable == true) {
                            Testing(
                                isSubscribed = testingProgramSubscribed
                                    ?: app.testingProgram!!.isSubscribed,
                                isUpdating = testingProgramUpdating,
                                onTestingSubscriptionChange = onTestingSubscriptionChange
                            )
                        }
                    }

                    item {
                        Compatibility(needsGms = app.requiresGMS(), plexusScores = plexusScores)
                    }

                    item {
                        SectionHeader(
                            title = stringResource(R.string.details_permission),
                            subtitle = if (app.permissions.isNotEmpty()) {
                                stringResource(R.string.permissions_requested, app.permissions.size)
                            } else {
                                stringResource(R.string.details_no_permission)
                            },
                            onClick = if (app.permissions.isNotEmpty()) {
                                { showExtraPane(ExtraScreen.Permission) }
                            } else {
                                null
                            }
                        )
                    }

                    item {
                        if (dataSafetyReport != null) {
                            DataSafety(
                                report = dataSafetyReport,
                                privacyPolicyUrl = app.privacyPolicyUrl
                            )
                        }
                    }

                    item {
                        Privacy(
                            report = exodusReport,
                            onNavigateToDetailsExodus = if (exodusReport != null &&
                                exodusReport.id != -1
                            ) {
                                { showExtraPane(ExtraScreen.Exodus) }
                            } else {
                                null
                            }
                        )
                    }

                    item {
                        DeveloperDetails(
                            address = app.developerAddress,
                            website = app.developerWebsite,
                            email = app.developerEmail
                        )
                    }

                    if (shouldShowMenuOnMainPane) {
                        suggestionClusterItems(
                            suggestionsBundle = suggestionsBundle,
                            onAppClick = { onNavigateTo(Destination.AppDetails(it.packageName)) },
                            onClusterScrolled = onLoadMoreCluster
                        )
                    }
                }
                ScrollHint(
                    listState = listState,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }

    @Composable
    fun SupportingPane() {
        Scaffold(
            topBar = {
                TopAppBar(
                    showNavigationIcon = false,
                    actions = { if (!shouldShowMenuOnMainPane) SetupMenu() }
                )
            }
        ) { paddingValues ->
            val hasContent = suggestionsBundle == null || suggestionsBundle.hasClusters()
            if (hasContent) {
                StreamCarousel(
                    modifier = Modifier.padding(paddingValues),
                    streamBundle = suggestionsBundle,
                    filterSingleAppClusters = false,
                    onAppClick = { onNavigateTo(Destination.AppDetails(it.packageName)) },
                    onClusterScrolled = onLoadMoreCluster
                )
            }
        }
    }

    @Composable
    fun ExtraPane(screen: NavKey) = when (screen) {
        is ExtraScreen.Review -> ReviewScreen(
            packageName = app.packageName
        )

        is ExtraScreen.Exodus -> ExodusScreen(
            packageName = app.packageName
        )

        is ExtraScreen.More -> MoreScreen(
            packageName = app.packageName,
            onNavigateTo = onNavigateTo
        )

        is ExtraScreen.Permission -> PermissionScreen(
            packageName = app.packageName
        )

        is ExtraScreen.Screenshot -> ScreenshotScreen(
            packageName = app.packageName,
            index = screen.index
        )

        is ExtraScreen.ManualDownload -> ManualDownloadScreen(
            packageName = app.packageName,
            onRequestInstall = { requestedApp -> onInstall(requestedApp) }
        )

        is ExtraScreen.MicroG -> MicroGScreen(
            packageName = app.packageName,
            onProceed = { onInstall(ignoreMicroG = true) }
        )

        // Play gives a developer id only to developers with a developer page; the rest are
        // reached through a pub: search on their name
        is Screen.DevProfile -> if (app.developerId.isNotBlank()) {
            DevProfileScreen(
                developerId = app.developerId,
                onNavigateTo = onNavigateTo,
                fallbackTitle = app.developerName
            )
        } else {
            DevProfileScreen(
                publisherId = app.developerName,
                onNavigateTo = onNavigateTo
            )
        }

        is Screen.PermissionRationale -> PermissionRationaleScreen(
            requiredPermissions = screen.requiredPermissions,
            onPermissionCallback = { type ->
                val isStoragePermission = type == PermissionType.EXTERNAL_STORAGE ||
                    type == PermissionType.STORAGE_MANAGER
                if (isStoragePermission && isGranted(context, type)) {
                    showRestartDialog = true
                } else {
                    onInstall()
                }
            }
        )

        else -> {}
    }

    NavigableSupportingPaneScaffold(
        navigator = scaffoldNavigator,
        mainPane = { AnimatedPane { MainPane() } },
        supportingPane = { AnimatedPane { SupportingPane() } },
        extraPane = {
            scaffoldNavigator.currentDestination?.contentKey?.let { screen ->
                AnimatedPane { ExtraPane(screen) }
            }
        }
    )
}

/**
 * Renders the suggestion stream as cluster rows inside the parent [LazyColumn].
 * Shows a shimmer placeholder while the bundle is still loading (`null`).
 */
private fun LazyListScope.suggestionClusterItems(
    suggestionsBundle: StreamBundle?,
    onAppClick: (App) -> Unit,
    onClusterScrolled: (StreamCluster) -> Unit
) {
    if (suggestionsBundle == null) {
        item(key = "suggestions-shimmer") { ShimmerCarouselSection() }
        return
    }

    val clusters = suggestionsBundle.clusters.distinctBy { it.id }.filter {
        it.title.isNotBlank() && it.apps.isNotEmpty()
    }

    clusters.forEach { cluster ->
        item(key = "cluster-header-${cluster.id}") {
            SectionHeader(title = cluster.title)
        }
        item(key = "cluster-row-${cluster.id}") {
            ClusterRow(
                cluster = cluster,
                onAppClick = onAppClick,
                onClusterScrolled = onClusterScrolled
            )
        }
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@PreviewScreenSizes
@Composable
private fun AppDetailsScreenPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    ScreenContentApp(
        app = app,
        isAnonymous = false,
        canDownload = true,
        suggestionsBundle = StreamBundle(
            id = "suggestions",
            clusters = listOf(
                StreamCluster(
                    id = "1",
                    title = "Similar apps",
                    apps = List(8) { app.copy(id = it) }
                ),
                StreamCluster(
                    id = "2",
                    title = "More by ${app.developerName}",
                    apps = List(5) { app.copy(id = 100 + it) }
                )
            )
        )
    )
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview
@Composable
private fun AppDetailsScreenPreviewLoading() {
    ScreenContentLoading()
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview
@Composable
private fun AppDetailsScreenPreviewError() {
    ScreenContentError()
}
