/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.navigation

import android.content.Intent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.metadata
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.aurora.Constants.PACKAGE_NAME_GMS
import com.aurora.extensions.toast
import com.aurora.store.AuroraApp
import com.aurora.store.ComposeActivity
import com.aurora.store.R
import com.aurora.store.compose.ui.about.AboutScreen
import com.aurora.store.compose.ui.accounts.AccountsScreen
import com.aurora.store.compose.ui.accounts.GoogleLoginScreen
import com.aurora.store.compose.ui.blacklist.BlacklistScreen
import com.aurora.store.compose.ui.commons.CategoryBrowseScreen
import com.aurora.store.compose.ui.commons.ExpandedStreamBrowseScreen
import com.aurora.store.compose.ui.commons.PermissionRationaleScreen
import com.aurora.store.compose.ui.commons.StreamBrowseScreen
import com.aurora.store.compose.ui.details.AppDetailsScreen
import com.aurora.store.compose.ui.dev.DevProfileScreen
import com.aurora.store.compose.ui.dispenser.DispenserScreen
import com.aurora.store.compose.ui.downloads.DownloadsScreen
import com.aurora.store.compose.ui.favourite.FavouriteScreen
import com.aurora.store.compose.ui.installed.InstalledScreen
import com.aurora.store.compose.ui.main.MainScreen
import com.aurora.store.compose.ui.onboarding.OnboardingScreen
import com.aurora.store.compose.ui.preferences.SettingsScreen
import com.aurora.store.compose.ui.preferences.UIPreferenceScreen
import com.aurora.store.compose.ui.preferences.installation.InstallationPreferenceScreen
import com.aurora.store.compose.ui.preferences.installation.InstallerScreen
import com.aurora.store.compose.ui.preferences.network.NetworkPreferenceScreen
import com.aurora.store.compose.ui.preferences.updates.SourceFiltersScreen
import com.aurora.store.compose.ui.preferences.updates.UpdatesPreferenceScreen
import com.aurora.store.compose.ui.search.SearchScreen
import com.aurora.store.compose.ui.splash.SplashScreen
import com.aurora.store.compose.ui.spoof.SpoofScreen
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.data.model.AccountType
import com.aurora.store.data.providers.AccountProvider
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.Preferences

/**
 * Navigation display for compose screens
 * @param startDestination Starting destination for the activity/app
 */
@Composable
fun NavDisplay(startDestination: NavKey) {
    val backstack = rememberNavBackStack(startDestination)
    val context = LocalContext.current

    fun isMicroGAuthInvalidated(): Boolean =
        Preferences.getBoolean(context, Preferences.PREFERENCE_AUTH_VIA_MICROG, false) &&
            AccountProvider.getAccountType(context) == AccountType.GOOGLE &&
            !PackageUtil.hasSupportedMicroGVariant(context)

    fun handleMicroGRemoved() {
        context.toast(R.string.microg_removed_auth_warning)
        AccountProvider.logout(context)
        val intent = Intent(context, ComposeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    // Check every time the screen resumes in case microG was removed while Aurora was in background.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (isMicroGAuthInvalidated()) handleMicroGRemoved()
    }

    // Also react immediately if the GMS package is uninstalled while Aurora is in the foreground.
    LaunchedEffect(Unit) {
        AuroraApp.events.installerEvent.collect { event ->
            if (event is InstallerEvent.Uninstalled && event.packageName == PACKAGE_NAME_GMS) {
                if (isMicroGAuthInvalidated()) handleMicroGRemoved()
            }
        }
    }

    fun navigate(destination: Destination) {
        when (destination) {
            Destination.Splash -> {
                // Clear the backstack when navigating to Splash to prevent going back to the previous screen when the user is sent back to the splash screen (e.g. after logout).
                backstack.clear()
                backstack.add(Screen.Splash)
            }

            is Destination.Main -> {
                // Clear the backstack when navigating to Main to prevent going back to the splash screen or other screens.
                backstack.clear()
                backstack.add(Screen.Main(destination.initialTab))
            }

            is Destination.ExpandedStreamBrowse -> backstack.add(
                Screen.ExpandedStreamBrowse(destination.title, destination.browseUrl)
            )

            is Destination.CategoryBrowse -> backstack.add(
                Screen.CategoryBrowse(destination.category.title, destination.category.browseUrl)
            )

            is Destination.PermissionRationale -> backstack.add(
                Screen.PermissionRationale(destination.permissions)
            )

            is Destination.AppDetails -> backstack.add(Screen.AppDetails(destination.packageName))
            is Destination.DevProfile -> backstack.add(Screen.DevProfile(destination.devId))
            is Destination.AppUpdate -> Unit
            is Destination.StreamBrowse -> backstack.add(Screen.StreamBrowse(destination.cluster))

            Destination.Search -> backstack.add(Screen.Search)
            Destination.Downloads -> backstack.add(Screen.Downloads)
            Destination.Accounts -> backstack.add(Screen.Accounts)
            Destination.GoogleLogin -> backstack.add(Screen.GoogleLogin)
            Destination.About -> backstack.add(Screen.About)
            Destination.Favourite -> backstack.add(Screen.Favourite)
            Destination.Spoof -> backstack.add(Screen.Spoof)
            Destination.Installed -> backstack.add(Screen.Installed)
            Destination.Blacklist -> backstack.add(Screen.Blacklist)
            Destination.Settings -> backstack.add(Screen.Settings)
            Destination.InstallationPreference -> backstack.add(Screen.InstallationPreference)
            Destination.Installer -> backstack.add(Screen.Installer)
            Destination.NetworkPreference -> backstack.add(Screen.NetworkPreference)
            Destination.Dispenser -> backstack.add(Screen.Dispenser)
            Destination.UIPreference -> backstack.add(Screen.UIPreference)
            Destination.UpdatesPreference -> backstack.add(Screen.UpdatesPreference)
            Destination.SourceFilters -> backstack.add(Screen.SourceFilters)
        }
    }

    NavDisplay(
        onBack = { backstack.removeLastOrNull() },
        backStack = backstack,
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
            entry<Screen.Main> { screen ->
                MainScreen(
                    initialTab = screen.initialTab,
                    onNavigateTo = ::navigate
                )
            }

            entry<Screen.AppDetails> { screen ->
                AppDetailsScreen(
                    packageName = screen.packageName,
                    onNavigateTo = ::navigate
                )
            }

            entry<Screen.DevProfile> { screen ->
                DevProfileScreen(
                    developerId = screen.developerId,
                    onNavigateTo = ::navigate
                )
            }

            entry<Screen.PermissionRationale> { screen ->
                PermissionRationaleScreen(
                    requiredPermissions = screen.requiredPermissions
                )
            }

            entry<Screen.StreamBrowse> { screen ->
                StreamBrowseScreen(
                    streamCluster = screen.streamCluster,
                    onNavigateTo = ::navigate
                )
            }

            entry<Screen.ExpandedStreamBrowse> { screen ->
                ExpandedStreamBrowseScreen(
                    browseUrl = screen.browseUrl,
                    defaultTitle = screen.title,
                    onNavigateTo = ::navigate
                )
            }

            entry<Screen.CategoryBrowse> { screen ->
                CategoryBrowseScreen(
                    title = screen.title,
                    browseUrl = screen.browseUrl,
                    onNavigateTo = ::navigate
                )
            }

            entry<Screen.InstallationPreference> {
                InstallationPreferenceScreen(onNavigateTo = ::navigate)
            }

            entry<Screen.Search>(
                metadata = metadata {
                    put(NavDisplay.TransitionKey) {
                        fadeIn() togetherWith
                            ExitTransition.KeepUntilTransitionsFinished
                    }
                    put(NavDisplay.PopTransitionKey) {
                        EnterTransition.None togetherWith
                            slideOutVertically(targetOffsetY = { it })
                    }
                    put(NavDisplay.PredictivePopTransitionKey) {
                        EnterTransition.None togetherWith
                            slideOutVertically(targetOffsetY = { it })
                    }
                }
            ) { SearchScreen() }

            entry<Screen.Splash> { SplashScreen(onNavigateTo = ::navigate) }
            entry<Screen.Onboarding> { OnboardingScreen() }
            entry<Screen.Blacklist> { BlacklistScreen() }
            entry<Screen.Downloads> { DownloadsScreen(onNavigateTo = ::navigate) }
            entry<Screen.Accounts> { AccountsScreen(onNavigateTo = ::navigate) }
            entry<Screen.GoogleLogin> { GoogleLoginScreen(onNavigateTo = ::navigate) }
            entry<Screen.About> { AboutScreen() }
            entry<Screen.Favourite> { FavouriteScreen(onNavigateTo = ::navigate) }
            entry<Screen.Spoof> { SpoofScreen(onNavigateTo = ::navigate) }
            entry<Screen.Dispenser> { DispenserScreen() }
            entry<Screen.Installer> { InstallerScreen() }
            entry<Screen.Installed> { InstalledScreen(onNavigateTo = ::navigate) }
            entry<Screen.Settings> { SettingsScreen(onNavigateTo = ::navigate) }
            entry<Screen.NetworkPreference> { NetworkPreferenceScreen(onNavigateTo = ::navigate) }
            entry<Screen.UIPreference> { UIPreferenceScreen() }
            entry<Screen.UpdatesPreference> { UpdatesPreferenceScreen(onNavigateTo = ::navigate) }
            entry<Screen.SourceFilters> { SourceFiltersScreen() }
        }
    )
}
