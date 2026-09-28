/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurora.extensions.requiresObbDir
import com.aurora.store.MainViewModel
import com.aurora.store.R
import com.aurora.store.compose.composable.InsufficientStorageDialog
import com.aurora.store.compose.composable.TrackerUpdateWarningDialog
import com.aurora.store.compose.composition.LocalNetworkStatus
import com.aurora.store.compose.navigation.Destination
import com.aurora.store.compose.ui.apps.AppsGamesScreen
import com.aurora.store.compose.ui.commons.MoreSheet
import com.aurora.store.compose.ui.commons.NetworkScreen
import com.aurora.store.compose.ui.sheets.AppUpdateSheet
import com.aurora.store.compose.ui.search.MainSearchButton
import com.aurora.store.compose.ui.search.SearchScreen
import com.aurora.store.compose.ui.search.searchPanelMotionSpec
import com.aurora.store.compose.ui.updates.UpdatesScreen
import com.aurora.store.data.model.ExodusTracker
import com.aurora.store.data.model.NetworkStatus
import com.aurora.store.data.model.PermissionType
import com.aurora.store.data.model.StorageRequirement
import com.aurora.store.data.providers.PermissionProvider.Companion.isGranted
import com.aurora.store.data.room.update.Update
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_WARN_TRACKERS
import com.aurora.store.util.StorageUtil
import com.aurora.store.viewmodel.all.UpdatesViewModel
import com.aurora.store.viewmodel.notifications.NotificationsViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private enum class MainTab(
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int
) {
    APPS(R.string.title_apps, R.drawable.ic_apps),
    GAMES(R.string.title_games, R.drawable.ic_games),
    UPDATES(R.string.title_updates, R.drawable.ic_updates)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainScreen(
    initialTab: Int = 0,
    mainViewModel: MainViewModel = hiltViewModel(),
    updatesViewModel: UpdatesViewModel = hiltViewModel(),
    notificationsViewModel: NotificationsViewModel = hiltViewModel(),
    onNavigateTo: (Destination) -> Unit = {}
) {
    val context = LocalContext.current
    val networkStatus = LocalNetworkStatus.current
    val updates by mainViewModel.updateHelper.updates.collectAsStateWithLifecycle(
        initialValue = null
    )
    val updateCount = updates?.size ?: 0
    val notificationCount by notificationsViewModel.unreadCount.collectAsStateWithLifecycle()
    val downloads by updatesViewModel.downloadsList.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = initialTab.coerceIn(
            0,
            MainTab.entries.size - 1
        )
    ) {
        MainTab.entries.size
    }
    val floatingToolbarShape = FloatingToolbarDefaults.ContainerShape
    val surfaceColor = MaterialTheme.colorScheme.surface
    val bottomBarGradient = remember(surfaceColor) {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.0f to surfaceColor.copy(alpha = 0.0f),
                0.55f to surfaceColor.copy(alpha = 0.72f),
                1.0f to surfaceColor.copy(alpha = 0.92f)
            )
        )
    }
    val topAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val topAppBarShape = RoundedCornerShape(
        bottomStart = dimensionResource(R.dimen.radius_large),
        bottomEnd = dimensionResource(R.dimen.radius_large)
    )

    var showMoreSheet by remember { mutableStateOf(false) }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf<String?>(null) }
    var appUpdateTarget by remember { mutableStateOf<Update?>(null) }
    var trackerWarning by remember {
        mutableStateOf<Pair<Update, List<ExodusTracker>>?>(null)
    }
    var storageWarning by remember { mutableStateOf<StorageRequirement?>(null) }
    val checkingJobs = remember { mutableStateMapOf<String, Job>() }

    // A blocked update never produces a download, so the effect below can't clear its marker.
    LaunchedEffect(Unit) {
        updatesViewModel.storageWarning.collect {
            storageWarning = it
            checkingJobs.clear()
        }
    }

    // Once the download a check kicked off actually appears, drop the "checking" marker so the
    // item's in-progress state is driven purely by the download (no flash back to "Update").
    LaunchedEffect(downloads) {
        checkingJobs.keys.toList().forEach { pkg ->
            if (downloads.any { it.packageName == pkg && !it.isFinished }) {
                checkingJobs.remove(pkg)
            }
        }
    }

    fun handleNavigation(destination: Destination) {
        when (destination) {
            is Destination.AppUpdate -> appUpdateTarget = destination.update
            else -> onNavigateTo(destination)
        }
    }

    fun handleMainSearchNavigation(destination: Destination) {
        if (destination is Destination.Search) {
            searchQuery = destination.query
            showSearch = true
        } else {
            handleNavigation(destination)
        }
    }

    fun performUpdate(update: Update) {
        if (update.fileList.requiresObbDir() &&
            !isGranted(context, PermissionType.STORAGE_MANAGER)
        ) {
            checkingJobs.remove(update.packageName)
            onNavigateTo(
                Destination.PermissionRationale(setOf(PermissionType.STORAGE_MANAGER))
            )
        } else {
            updatesViewModel.download(update)
        }
    }

    if (networkStatus == NetworkStatus.UNAVAILABLE) {
        NetworkScreen()
        return
    }

    if (showMoreSheet) {
        MoreSheet(
            onDismiss = { showMoreSheet = false },
            onNavigateTo = { destination ->
                showMoreSheet = false
                onNavigateTo(destination)
            }
        )
    }

    appUpdateTarget?.let { app ->
        AppUpdateSheet(
            update = app,
            onDismiss = { appUpdateTarget = null },
            onNavigateTo = { destination ->
                appUpdateTarget = null
                onNavigateTo(destination)
            }
        )
    }

        Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
            containerColor = Color.Transparent,
            topBar = {
                MediumTopAppBar(
                    modifier = Modifier.clip(topAppBarShape),
                    title = {
                        Text(
                            text = stringResource(MainTab.entries[pagerState.currentPage].labelRes),
                            style = MaterialTheme.typography.headlineSmall
                        )
                    },
                    actions = {
                        IconButton(onClick = { onNavigateTo(Destination.Notifications) }) {
                            BadgedBox(
                                badge = {
                                    if (notificationCount > 0) Badge { Text("$notificationCount") }
                                }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_notifications),
                                    contentDescription = stringResource(R.string.title_notifications)
                                )
                            }
                        }
                        IconButton(onClick = { onNavigateTo(Destination.Downloads) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_download_manager),
                                contentDescription = stringResource(R.string.title_download_manager)
                            )
                        }
                        IconButton(onClick = { showMoreSheet = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_settings_account),
                                contentDescription = stringResource(R.string.title_more)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = surfaceColor,
                        scrolledContainerColor = surfaceColor
                    ),
                    scrollBehavior = topAppBarScrollBehavior
                )
            }
        ) { paddingValues ->
            val topInset = paddingValues.calculateTopPadding()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topInset)
                    .consumeWindowInsets(PaddingValues(top = topInset))
            ) {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = false,
                    beyondViewportPageCount = MainTab.entries.size - 1,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (MainTab.entries[page]) {
                        MainTab.APPS -> AppsGamesScreen(
                            pageType = 0,
                            onNavigateTo = onNavigateTo
                        )
                        MainTab.GAMES -> AppsGamesScreen(
                            pageType = 1,
                            onNavigateTo = ::handleNavigation
                        )
                        MainTab.UPDATES -> {
                            UpdatesScreen(
                                viewModel = updatesViewModel,
                                onNavigateTo = ::handleNavigation,
                                onRequestUpdate = { update ->
                                    if (!Preferences.getBoolean(
                                            context,
                                            PREFERENCE_UPDATES_WARN_TRACKERS,
                                            false
                                        )
                                    ) {
                                        performUpdate(update)
                                    } else {
                                        val job = coroutineScope.launch {
                                            val installedVc = PackageUtil.getInstalledVersionCode(
                                                context,
                                                update.packageName
                                            )
                                            val trackers = updatesViewModel.getNewTrackers(
                                                update.packageName,
                                                installedVc
                                            )
                                            if (trackers.isEmpty()) {
                                                performUpdate(update)
                                            } else {
                                                trackerWarning = update to trackers
                                            }
                                        }
                                        checkingJobs[update.packageName] = job
                                    }
                                },
                                onRequestUpdateAll = { selectedUpdates ->
                                    val needsObb = selectedUpdates.any {
                                        it.fileList.requiresObbDir()
                                    }
                                    if (needsObb &&
                                        !isGranted(context, PermissionType.STORAGE_MANAGER)
                                    ) {
                                        onNavigateTo(
                                            Destination.PermissionRationale(
                                                setOf(PermissionType.STORAGE_MANAGER)
                                            )
                                        )
                                    } else {
                                        updatesViewModel.downloadAll(selectedUpdates)
                                    }
                                },
                                onCancelUpdate = { packageName ->
                                    if (downloads.any {
                                            it.packageName == packageName && !it.isFinished
                                        }
                                    ) {
                                        checkingJobs.remove(packageName)
                                        updatesViewModel.cancelDownload(packageName)
                                    } else {
                                        checkingJobs.remove(packageName)?.cancel()
                                    }
                                },
                                onCancelAll = { updatesViewModel.cancelAll() },
                                checkingPackages = checkingJobs.keys,
                                bottomContentPadding = 148.dp
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = !showSearch,
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.BottomCenter)
                .fillMaxWidth(),
            enter = fadeIn(animationSpec = tween(durationMillis = 140)),
            exit = fadeOut(animationSpec = tween(durationMillis = 140))
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Keep the feed visible beneath the lower edge while fading it into the toolbar.
                Box(
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(144.dp)
                        .background(bottomBarGradient)
                )

                Box(
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.BottomCenter)
                        .fillMaxWidth(),
                    contentAlignment = androidx.compose.ui.Alignment.BottomCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HorizontalFloatingToolbar(
                                expanded = true,
                                shape = floatingToolbarShape,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                expandedShadowElevation = 8.dp,
                                collapsedShadowElevation = 8.dp,
                                content = {
                                    MainTab.entries.forEachIndexed { index, tab ->
                                        FloatingMainTab(
                                            tab = tab,
                                            selected = pagerState.targetPage == index,
                                            updateCount = updateCount,
                                            shape = floatingToolbarShape,
                                            onClick = {
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(
                                                        page = index,
                                                        animationSpec = spring(
                                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                                            stiffness = Spring.StiffnessMediumLow
                                                        )
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                            )
                            MainSearchButton(onNavigateTo = ::handleMainSearchNavigation)
                        }
                        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showSearch,
            modifier = Modifier.fillMaxSize(),
            enter = slideInVertically(searchPanelMotionSpec) { it },
            exit = slideOutVertically(searchPanelMotionSpec) { it }
        ) {
            SearchScreen(
                initialQuery = searchQuery,
                onNavigateBack = { showSearch = false },
                onNavigateTo = ::handleNavigation
            )
        }
    }

    trackerWarning?.let { (update, trackers) ->
        TrackerUpdateWarningDialog(
            trackers = trackers,
            onConfirm = {
                val pending = update
                trackerWarning = null
                performUpdate(pending)
            },
            onDismiss = {
                trackerWarning = null
                checkingJobs.remove(update.packageName)
            }
        )
    }

    storageWarning?.let { requirement ->
        InsufficientStorageDialog(
            requirement = requirement,
            onFreeUpSpace = {
                storageWarning = null
                StorageUtil.openFreeUpSpace(context)
            },
            onDismiss = { storageWarning = null }
        )
    }
}

@Composable
private fun FloatingMainTab(
    tab: MainTab,
    selected: Boolean,
    updateCount: Int,
    shape: androidx.compose.ui.graphics.Shape,
    onClick: () -> Unit
) {
    // Drive color, icon visibility, and intrinsic width from one transition. This keeps the
    // measured end state identical to its animated target, avoiding a final clip/snap caused by
    // separately animating icon visibility and parent content size.
    val transition = updateTransition(targetState = selected, label = "main-tab-selection")
    val selectionMotion = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
    val colorMotion = spring<Color>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
    val contentColor by transition.animateColor(
        transitionSpec = { colorMotion },
        label = "main-tab-content-color"
    ) { isSelected ->
        if (isSelected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    }
    val containerColor by transition.animateColor(
        transitionSpec = { colorMotion },
        label = "main-tab-container-color"
    ) { isSelected ->
        if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    }
    val horizontalPadding by transition.animateDp(
        transitionSpec = {
            spring<Dp>(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        },
        label = "main-tab-horizontal-padding"
    ) { isSelected -> if (isSelected) 16.dp else 12.dp }
    val iconSlotWidth by transition.animateDp(
        transitionSpec = {
            spring<Dp>(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        },
        label = "main-tab-icon-slot-width"
    ) { isSelected -> if (isSelected) 28.dp else 0.dp }
    val iconAlpha by transition.animateFloat(
        transitionSpec = { selectionMotion },
        label = "main-tab-icon-alpha"
    ) { isSelected -> if (isSelected) 1f else 0f }
    val iconScale by transition.animateFloat(
        transitionSpec = { selectionMotion },
        label = "main-tab-icon-scale"
    ) { isSelected -> if (isSelected) 1f else 0.82f }
    Row(
        modifier = Modifier
            .clip(shape)
            .background(
                color = containerColor,
                shape = shape
            )
            .selectable(
                selected = selected,
                role = Role.Tab,
                onClick = onClick
            )
            .padding(horizontal = horizontalPadding, vertical = 12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.width(iconSlotWidth),
            contentAlignment = androidx.compose.ui.Alignment.CenterStart
        ) {
            Icon(
                painter = painterResource(tab.iconRes),
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer {
                        alpha = iconAlpha
                        scaleX = iconScale
                        scaleY = iconScale
                    },
                tint = contentColor
            )
        }
        Text(
            text = stringResource(tab.labelRes),
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            maxLines = 1
        )
        if (tab == MainTab.UPDATES && updateCount > 0) {
            Spacer(Modifier.width(8.dp))
            Badge { Text("$updateCount") }
        }
    }
}
