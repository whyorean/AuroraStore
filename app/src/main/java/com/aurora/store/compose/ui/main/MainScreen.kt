/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurora.extensions.requiresObbDir
import com.aurora.store.MainViewModel
import com.aurora.store.R
import com.aurora.store.compose.composable.InsufficientStorageDialog
import com.aurora.store.compose.composable.TopAppBar
import com.aurora.store.compose.composable.TrackerUpdateWarningDialog
import com.aurora.store.compose.composition.LocalNetworkStatus
import com.aurora.store.compose.navigation.Destination
import com.aurora.store.compose.ui.apps.AppsGamesScreen
import com.aurora.store.compose.ui.commons.MoreSheet
import com.aurora.store.compose.ui.commons.NetworkScreen
import com.aurora.store.compose.ui.sheets.AppUpdateSheet
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

    var showMoreSheet by remember { mutableStateOf(false) }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(MainTab.entries[pagerState.currentPage].labelRes),
                showNavigationIcon = false,
                actions = {
                    IconButton(onClick = { onNavigateTo(Destination.Notifications) }) {
                        BadgedBox(
                            badge = {
                                if (notificationCount > 0) Badge { Text("$notificationCount") }
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_notifications_black_24dp),
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
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigateTo(Destination.Search) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_round_search),
                    contentDescription = stringResource(R.string.action_search)
                )
            }
        },
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                        },
                        icon = {
                            if (tab == MainTab.UPDATES && updateCount > 0) {
                                BadgedBox(badge = { Badge { Text("$updateCount") } }) {
                                    Icon(
                                        painter = painterResource(tab.iconRes),
                                        contentDescription = null
                                    )
                                }
                            } else {
                                Icon(
                                    painter = painterResource(tab.iconRes),
                                    contentDescription = null
                                )
                            }
                        },
                        label = { Text(stringResource(tab.labelRes)) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .fillMaxSize()
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
                            checkingPackages = checkingJobs.keys
                        )
                    }
                }
            }
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
