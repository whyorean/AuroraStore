/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.home.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.aurora.store.R

/**
 * Screens that can be shown in home
 */
enum class HomeScreen(@StringRes val localized: Int, @DrawableRes val icon: Int) {
    APPS(R.string.title_apps, R.drawable.ic_apps),
    GAMES(R.string.title_games, R.drawable.ic_games),
    UPDATES(R.string.title_updates, R.drawable.ic_updates)
}
