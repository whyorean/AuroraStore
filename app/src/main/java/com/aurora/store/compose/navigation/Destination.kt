/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.navigation

import com.aurora.gplayapi.data.models.Category
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.store.data.model.MinimalApp
import com.aurora.store.data.model.PermissionType

/**
 * All navigation actions available to composable screens.
 * Screens emit one of these via a single `onNavigateTo: (Destination) -> Unit` callback.
 */
sealed class Destination {
    data object Splash : Destination()
    data class Main(val initialTab: Int) : Destination()

    data class AppDetails(val packageName: String) : Destination()
    data class DevProfile(val devId: String) : Destination()
    data class AppMenu(val app: MinimalApp) : Destination()

    data object Search : Destination()
    data object Downloads : Destination()

    data class StreamBrowse(val cluster: StreamCluster) : Destination()
    data class ExpandedStreamBrowse(val title: String, val browseUrl: String) : Destination()
    data class CategoryBrowse(val category: Category) : Destination()
    data class PermissionRationale(val permissions: Set<PermissionType>) : Destination()

    data object Accounts : Destination()
    data object GoogleLogin : Destination()
    data object About : Destination()
    data object Favourite : Destination()
    data object Spoof : Destination()
    data object Installed : Destination()
    data object Blacklist : Destination()

    data object Settings : Destination()
    data object InstallationPreference : Destination()
    data object Installer : Destination()
    data object NetworkPreference : Destination()
    data object Dispenser : Destination()
    data object UIPreference : Destination()
    data object UpdatesPreference : Destination()
}
