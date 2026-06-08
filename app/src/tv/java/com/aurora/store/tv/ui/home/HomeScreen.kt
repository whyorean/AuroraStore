/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.tv.ui.home

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.tv.material3.Icon
import androidx.tv.material3.NavigationDrawer
import androidx.tv.material3.NavigationDrawerItem
import androidx.tv.material3.Text
import com.aurora.gplayapi.helpers.contracts.StreamContract
import com.aurora.store.HomeStash
import com.aurora.store.R
import com.aurora.store.data.model.ViewState
import com.aurora.store.viewmodel.homestream.StreamViewModel
import kotlinx.coroutines.android.awaitFrame

private enum class HomeSection { APPS, GAMES, SEARCH, UPDATES }

@Composable
fun HomeScreen(onAppClick: (String) -> Unit) {
    // Saved so navigating into an app's details and back restores the section the user left from
    // (e.g. Search), rather than resetting to Apps.
    var section by rememberSaveable { mutableStateOf(HomeSection.APPS) }

    val contentFocusRequester = remember { FocusRequester() }

    // Returning from app details rebuilds this composition; without restoring focus it defaults into
    // the nav drawer, which expands it. Push focus into the content group instead so the drawer
    // stays collapsed, matching the state the user left. No-op on first launch while content is
    // still loading (no focusable child yet).
    LaunchedEffect(Unit) {
        awaitFrame()
        runCatching { contentFocusRequester.requestFocus() }
    }

    // Apps and Games each get their own StreamViewModel instance (keyed), mirroring the phone's
    // AppsGamesScreen. A single shared VM would post ViewState.Loading on every section switch and
    // blank the other category's content; isolated VMs keep each section's state intact.
    val appsViewModel: StreamViewModel = hiltViewModel(key = "tv_stream_apps")
    val gamesViewModel: StreamViewModel = hiltViewModel(key = "tv_stream_games")

    // Fetch each category once, on first visit, so returning to a section never re-flashes Loading.
    var appsLoaded by rememberSaveable { mutableStateOf(false) }
    var gamesLoaded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(section) {
        when (section) {
            HomeSection.APPS -> if (!appsLoaded) {
                appsViewModel.getStreamBundle(
                    StreamContract.Category.APPLICATION,
                    StreamContract.Type.HOME
                )
                appsLoaded = true
            }
            HomeSection.GAMES -> if (!gamesLoaded) {
                gamesViewModel.getStreamBundle(
                    StreamContract.Category.GAME,
                    StreamContract.Type.HOME
                )
                gamesLoaded = true
            }
            else -> Unit
        }
    }

    val appsState by appsViewModel.liveData.observeAsState()
    val gamesState by gamesViewModel.liveData.observeAsState()

    @Suppress("UNCHECKED_CAST")
    val appsStash = (appsState as? ViewState.Success<*>)?.data as? HomeStash

    @Suppress("UNCHECKED_CAST")
    val gamesStash = (gamesState as? ViewState.Success<*>)?.data as? HomeStash

    NavigationDrawer(
        modifier = Modifier.fillMaxSize(),
        drawerContent = { _ ->
            Column(modifier = Modifier.selectableGroup()) {
                NavigationDrawerItem(
                    selected = section == HomeSection.APPS,
                    onClick = { section = HomeSection.APPS },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_apps),
                            contentDescription = null
                        )
                    }
                ) {
                    Text(stringResource(R.string.title_apps))
                }

                NavigationDrawerItem(
                    selected = section == HomeSection.GAMES,
                    onClick = { section = HomeSection.GAMES },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_games),
                            contentDescription = null
                        )
                    }
                ) {
                    Text(stringResource(R.string.title_games))
                }

                NavigationDrawerItem(
                    selected = section == HomeSection.SEARCH,
                    onClick = { section = HomeSection.SEARCH },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_search_suggestion),
                            contentDescription = null
                        )
                    }
                ) {
                    Text(stringResource(R.string.action_search))
                }

                NavigationDrawerItem(
                    selected = section == HomeSection.UPDATES,
                    onClick = { section = HomeSection.UPDATES },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_updates),
                            contentDescription = null
                        )
                    }
                ) {
                    Text(stringResource(R.string.title_updates))
                }
            }
        }
    ) {
        Box(
            modifier = Modifier
                .focusGroup()
                .focusRequester(contentFocusRequester)
        ) {
            when (section) {
                HomeSection.APPS -> StreamRows(
                    streamBundle = appsStash?.get(StreamContract.Category.APPLICATION),
                    onAppClick = { app -> onAppClick(app.packageName) }
                )
                HomeSection.GAMES -> StreamRows(
                    streamBundle = gamesStash?.get(StreamContract.Category.GAME),
                    onAppClick = { app -> onAppClick(app.packageName) }
                )
                HomeSection.SEARCH -> SearchSection(onAppClick = onAppClick)
                HomeSection.UPDATES -> UpdatesSection(onAppClick = onAppClick)
            }
        }
    }
}
