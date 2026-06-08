/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.tv.navigation

import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.aurora.store.tv.ui.details.AppDetailsScreen
import com.aurora.store.tv.ui.home.HomeScreen
import com.aurora.store.tv.ui.login.LoginScreen
import com.aurora.store.tv.ui.onboarding.OnboardingScreen

/**
 * Navigation display for TV screens.
 * Mirrors the phone [NavDisplay] call shape from NavDisplay.kt.
 */
@Composable
fun Navigation(startDestination: Screen) {
    val backStack = rememberNavBackStack(startDestination)

    NavDisplay(
        onBack = { backStack.removeLastOrNull() },
        backStack = backStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        transitionSpec = {
            slideInHorizontally(
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f),
                initialOffsetX = { it }
            ) togetherWith slideOutHorizontally(targetOffsetX = { -it })
        },
        popTransitionSpec = {
            slideInHorizontally(
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f),
                initialOffsetX = { -it }
            ) togetherWith slideOutHorizontally(targetOffsetX = { it })
        },
        predictivePopTransitionSpec = {
            slideInHorizontally(
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f),
                initialOffsetX = { -it }
            ) togetherWith slideOutHorizontally(targetOffsetX = { it })
        },
        entryProvider = entryProvider {
            entry<Screen.Onboarding> {
                OnboardingScreen(
                    onFinished = {
                        backStack.clear()
                        backStack.add(Screen.Login)
                    }
                )
            }

            entry<Screen.Login> {
                LoginScreen(
                    onLoggedIn = {
                        backStack.clear()
                        backStack.add(Screen.Home)
                    }
                )
            }

            entry<Screen.Home> {
                HomeScreen(
                    onAppClick = { pkg -> backStack.add(Screen.AppDetails(pkg)) }
                )
            }

            entry<Screen.AppDetails> { key ->
                AppDetailsScreen(packageName = key.packageName)
            }
        }
    )
}
