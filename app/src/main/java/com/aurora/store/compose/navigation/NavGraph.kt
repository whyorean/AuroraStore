/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.aurora.extensions.parentViewModel
import com.aurora.store.compose.ui.commons.BlacklistScreen
import com.aurora.store.compose.ui.details.AppDetailsScreen
import com.aurora.store.compose.ui.details.DetailsExodusScreen
import com.aurora.store.compose.ui.details.DetailsMoreScreen
import com.aurora.store.compose.ui.details.DetailsReviewScreen
import com.aurora.store.compose.ui.details.DetailsScreenshotScreen

/**
 * Navigation graph for compose screens
 * @param navHostController [NavHostController] to navigate with compose
 * @param startDestination Starting destination for the activity/app
 */
@Composable
fun NavGraph(navHostController: NavHostController, startDestination: Screen) {
    // TODO: Drop this logic once everything is in compose
    val activity = LocalActivity.current
    fun onNavigateUp() {
        if (navHostController.previousBackStackEntry != null) {
            navHostController.navigateUp()
        } else {
            activity?.finish()
        }
    }

    NavHost(navController = navHostController, startDestination = startDestination) {
        composable<Screen.Blacklist> {
            BlacklistScreen(onNavigateUp = { onNavigateUp() })
        }

        composable<Screen.AppDetails> { backstackEntry ->
            val appDetails = backstackEntry.toRoute<Screen.AppDetails>()
            AppDetailsScreen(
                packageName = appDetails.packageName,
                viewModel = backstackEntry.parentViewModel(navHostController),
                onNavigateUp = { onNavigateUp() },
                onNavigateToDetailsMore = { navHostController.navigate(Screen.DetailsMore) },
                onNavigateToDetailsReview = { navHostController.navigate(Screen.DetailsReview) },
                onNavigateToDetailsExodus = { navHostController.navigate(Screen.DetailsExodus) },
                onNavigateToDetailsScreenshot = { index ->
                    navHostController.navigate(
                        Screen.DetailsScreenshot(index)
                    )
                }
            )
        }

        composable<Screen.DetailsExodus> { backstackEntry ->
            DetailsExodusScreen(
                onNavigateUp = { onNavigateUp() },
                viewModel = backstackEntry.parentViewModel(navHostController)
            )
        }

        composable<Screen.DetailsMore> { backstackEntry ->
            DetailsMoreScreen(
                onNavigateUp = { onNavigateUp() },
                viewModel = backstackEntry.parentViewModel(navHostController)
            )
        }

        composable<Screen.DetailsScreenshot> { backstackEntry ->
            val screenshotDetails = backstackEntry.toRoute<Screen.DetailsScreenshot>()
            DetailsScreenshotScreen(
                index = screenshotDetails.index,
                onNavigateUp = { onNavigateUp() },
                viewModel = backstackEntry.parentViewModel(navHostController)
            )
        }

        composable<Screen.DetailsReview> { backstackEntry ->
            DetailsReviewScreen(
                onNavigateUp = { onNavigateUp() },
                viewModel = backstackEntry.parentViewModel(navHostController)
            )
        }
    }
}
