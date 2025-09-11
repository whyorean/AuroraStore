/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import com.aurora.store.compose.ui.commons.BlacklistScreen
import com.aurora.store.compose.ui.details.AppDetailsScreen
import com.aurora.store.compose.ui.dev.DevProfileScreen
import com.aurora.store.compose.ui.commons.PermissionRationaleScreen
import com.aurora.store.compose.ui.search.SearchScreen

/**
 * Navigation display for compose screens
 * @param startDestination Starting destination for the activity/app
 */
@Composable
fun NavDisplay(startDestination: NavKey) {
    val backstack = rememberNavBackStack(startDestination)

    // TODO: Drop this logic once everything is in compose
    val activity = LocalActivity.current
    fun onNavigateUp() {
        if (backstack.size == 1) activity?.finish() else backstack.removeLastOrNull()
    }

    NavDisplay(
        backStack = backstack,
        entryDecorators = listOf(
            rememberSceneSetupNavEntryDecorator(),
            rememberSavedStateNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            entry<Screen.Blacklist> {
                BlacklistScreen(onNavigateUp = { onNavigateUp() })
            }

            entry<Screen.Search> {
                SearchScreen(onNavigateUp = { onNavigateUp() })
            }

            entry<Screen.AppDetails> { screen ->
                AppDetailsScreen(
                    packageName = screen.packageName,
                    onNavigateUp = { onNavigateUp() },
                    onNavigateToAppDetails = { packageName ->
                        backstack.add(Screen.AppDetails(packageName))
                    }
                )
            }

            entry<Screen.DevProfile> { screen ->
                DevProfileScreen(
                    developerId = screen.developerId,
                    onNavigateUp = { onNavigateUp() },
                    onNavigateToAppDetails = { packageName ->
                        backstack.add(Screen.AppDetails(packageName))
                    }
                )
            }

            entry<Screen.PermissionRationale> { screen ->
                PermissionRationaleScreen(
                    onNavigateUp = { onNavigateUp() },
                )
            }
        }
    )
}
