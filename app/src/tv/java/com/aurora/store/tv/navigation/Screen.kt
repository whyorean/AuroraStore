/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.tv.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Destinations for TV navigation in Compose
 */
@Serializable
sealed class Screen : NavKey {

    @Serializable
    data object Onboarding : Screen()

    @Serializable
    data object Login : Screen()

    @Serializable
    data object Home : Screen()

    @Serializable
    data class AppDetails(val packageName: String) : Screen()
}
