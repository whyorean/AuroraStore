/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.navigation3.runtime.NavKey
import com.aurora.store.compose.composable.TopAppBar
import com.aurora.store.compose.navigation.Screen
import com.aurora.store.compose.preview.PreviewTemplate
import com.aurora.store.compose.ui.home.menu.HomeContainerMenu
import com.aurora.store.compose.ui.home.menu.MenuItem
import com.aurora.store.compose.ui.home.navigation.HomeScreen

@Composable
fun HomeContainerScreen(onNavigateTo: (screen: NavKey) -> Unit) {
    val screens = listOf(
        HomeScreen.APPS,
        HomeScreen.GAMES,
        HomeScreen.UPDATES
    )

    ScreenContent(
        screens = screens,
        onNavigateTo = onNavigateTo
    )
}

@Composable
private fun ScreenContent(
    default: HomeScreen = HomeScreen.APPS,
    screens: List<HomeScreen> = emptyList(),
    onNavigateTo: (screen: NavKey) -> Unit = {}
) {
    var currentScreen by rememberSaveable { mutableStateOf(default) }
    var shouldShowMoreDialog by rememberSaveable { mutableStateOf(false) }

    if (shouldShowMoreDialog) {
        MoreDialog(
            onDismiss = { shouldShowMoreDialog = false },
            onNavigateTo = onNavigateTo
        )
    }

    @Composable
    fun SetupMenu() {
        HomeContainerMenu { menuItem ->
            when (menuItem) {
                MenuItem.DOWNLOADS -> onNavigateTo(Screen.Downloads)
                MenuItem.MORE -> {
                    shouldShowMoreDialog = true
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(currentScreen.localized),
                actions = { SetupMenu() }
            )
        }
    ) { paddingValues ->
        NavigationSuiteScaffold(
            modifier = Modifier.padding(paddingValues),
            navigationSuiteItems = {
                screens.forEach { screen ->
                    item(
                        icon = {
                            Icon(
                                painter = painterResource(screen.icon),
                                contentDescription = null
                            )
                        },
                        label = { Text(text = stringResource(screen.localized)) },
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen }
                    )
                }
            }
        ) {
            when (currentScreen) {
                HomeScreen.APPS -> AppsScreen()
                HomeScreen.GAMES -> GamesScreen()
                HomeScreen.UPDATES -> {
                    UpdatesScreen(
                        onNavigateToAppDetails = { packageName ->
                            onNavigateTo(Screen.AppDetails(packageName))
                        }
                    )
                }
            }
        }
    }
}

@PreviewScreenSizes
@Composable
private fun HomeContainerScreenPreview() {
    PreviewTemplate {
        val screens = listOf(
            HomeScreen.APPS,
            HomeScreen.GAMES,
            HomeScreen.UPDATES
        )
        ScreenContent(screens = screens)
    }
}
