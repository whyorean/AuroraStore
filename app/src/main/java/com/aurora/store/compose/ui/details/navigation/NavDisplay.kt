/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details.navigation

import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.SupportingPaneSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberSupportingPaneSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.aurora.Constants.SHARE_URL
import com.aurora.extensions.appInfo
import com.aurora.extensions.browse
import com.aurora.extensions.isWindowCompact
import com.aurora.extensions.requiresObbDir
import com.aurora.extensions.share
import com.aurora.gplayapi.data.models.App
import com.aurora.store.compose.navigation.Screen
import com.aurora.store.compose.ui.commons.PermissionRationaleScreen
import com.aurora.store.compose.ui.details.AppDetailsScreen
import com.aurora.store.compose.ui.details.ExodusScreen
import com.aurora.store.compose.ui.details.ManualDownloadScreen
import com.aurora.store.compose.ui.details.MoreScreen
import com.aurora.store.compose.ui.details.PermissionScreen
import com.aurora.store.compose.ui.details.ReviewScreen
import com.aurora.store.compose.ui.details.ScreenshotScreen
import com.aurora.store.compose.ui.details.SuggestionsScreen
import com.aurora.store.compose.ui.details.menu.AppDetailsMenu
import com.aurora.store.compose.ui.details.menu.MenuItem
import com.aurora.store.compose.ui.dev.DevProfileScreen
import com.aurora.store.data.model.PermissionType
import com.aurora.store.data.providers.PermissionProvider.Companion.isPermittedToInstall
import com.aurora.store.util.ShortcutManagerUtil
import com.aurora.store.viewmodel.details.AppDetailsViewModel

/**
 * Navigation display for AppDetails Screen
 *
 * When viewing details related to an app, a user can navigate to several screens among
 * which majority are related to the app, directly or indirectly. This navigation display
 * is supposed to manage those screens in its own separate backstack.
 */
@Composable
fun NavDisplay(
    packageName: String,
    onNavigateUp: () -> Unit,
    onNavigateToAppDetails: (packageName: String) -> Unit,
    forceSinglePane: Boolean = false,
    viewModel: AppDetailsViewModel = hiltViewModel(key = packageName),
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo()
) {
    val context = LocalContext.current
    val app by viewModel.app.collectAsStateWithLifecycle()

    // TODO: Make this adaptive so it doesn't looks bad; maybe use BoxWithConstraints
    var paneScaffoldDirective = calculatePaneScaffoldDirective(windowAdaptiveInfo)
    if (forceSinglePane) {
        paneScaffoldDirective = paneScaffoldDirective.copy(maxHorizontalPartitions = 1)
    }

    val startDestinations = listOfNotNull<NavKey>(
        Screen.AppDetails(packageName),
        if (windowAdaptiveInfo.isWindowCompact) SupportingScreen.Suggestions else null
    )
    val backstack = rememberNavBackStack(*startDestinations.toTypedArray())
    val supportingPaneSceneStrategy = rememberSupportingPaneSceneStrategy<NavKey>(
        directive = paneScaffoldDirective
    )

    fun onRequestNavigateUp() {
        if (!backstack.all { it in startDestinations }) {
            backstack.removeLastOrNull()
        } else {
            onNavigateUp()
        }
    }

    // TODO: Handle manual download on first time without permissions as app will change
    fun onInstall(requested: App = app!!) {
        if (isPermittedToInstall(context, requested)) {
            viewModel.enqueueDownload(requested)
            backstack.removeAll(setOf(ExtraScreen.ManualDownload, Screen.PermissionRationale))
        } else {
            val requiredPermissions = setOfNotNull(
                PermissionType.INSTALL_UNKNOWN_APPS,
                if (requested.fileList.requiresObbDir()) PermissionType.STORAGE_MANAGER else null,
                if (requested.fileList.requiresObbDir()) PermissionType.EXTERNAL_STORAGE else null
            )
            backstack.add(Screen.PermissionRationale(requiredPermissions = requiredPermissions))
        }
    }

    NavDisplay(
        backStack = backstack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        sceneStrategy = supportingPaneSceneStrategy,
        entryProvider = entryProvider {
            entry<Screen.AppDetails>(metadata = SupportingPaneSceneStrategy.mainPane()) {
                AppDetailsScreen(
                    packageName = packageName,
                    onNavigateUp = ::onRequestNavigateUp,
                    onNavigateToAppDetails = onNavigateToAppDetails,
                )
            }

            entry<SupportingScreen.Suggestions>(
                metadata = when {
                    windowAdaptiveInfo.isWindowCompact -> SupportingPaneSceneStrategy.extraPane()
                    else -> SupportingPaneSceneStrategy.supportingPane()
                }
            ) {
                SuggestionsScreen(
                    packageName = packageName,
                    onNavigateToAppDetails = onNavigateToAppDetails
                )
            }

            entry<ExtraScreen.Review>(metadata = SupportingPaneSceneStrategy.extraPane()) {
                ReviewScreen(
                    packageName = packageName,
                    onNavigateUp = ::onRequestNavigateUp
                )
            }

            entry<ExtraScreen.Exodus>(metadata = SupportingPaneSceneStrategy.extraPane()) {
                ExodusScreen(
                    packageName = packageName,
                    onNavigateUp = ::onRequestNavigateUp
                )
            }

            entry<ExtraScreen.More>(metadata = SupportingPaneSceneStrategy.extraPane()) {
                MoreScreen(
                    packageName = packageName,
                    onNavigateUp = ::onRequestNavigateUp,
                    onNavigateToAppDetails = onNavigateToAppDetails
                )
            }

            entry<ExtraScreen.Permission>(metadata = SupportingPaneSceneStrategy.extraPane()) {
                PermissionScreen(
                    packageName = packageName,
                    onNavigateUp = ::onRequestNavigateUp
                )
            }

            entry<ExtraScreen.Screenshot>(metadata = SupportingPaneSceneStrategy.extraPane()) {
                ScreenshotScreen(
                    packageName = packageName,
                    index = it.index,
                    onNavigateUp = ::onRequestNavigateUp
                )
            }

            entry<ExtraScreen.ManualDownload>(metadata = SupportingPaneSceneStrategy.extraPane()) {
                ManualDownloadScreen(
                    packageName = packageName,
                    onNavigateUp = ::onRequestNavigateUp,
                    onRequestInstall = { requestedApp -> onInstall(requestedApp) }
                )
            }

            // Independent screens but still navigated as extras as related to app //
            entry<Screen.DevProfile>(metadata = SupportingPaneSceneStrategy.extraPane()) {
                DevProfileScreen(
                    publisherId = app!!.developerName,
                    onNavigateUp = ::onRequestNavigateUp,
                    onNavigateToAppDetails = onNavigateToAppDetails
                )
            }

            entry<Screen.PermissionRationale>(metadata = SupportingPaneSceneStrategy.extraPane()) {
                PermissionRationaleScreen(
                    onNavigateUp = ::onRequestNavigateUp,
                    requiredPermissions = it.requiredPermissions,
                    onPermissionCallback = { onInstall() }
                )
            }
        }
    )
}
