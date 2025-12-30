/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.navigation

import android.os.Parcelable
import androidx.navigation3.runtime.NavKey
import com.aurora.store.data.model.PermissionType
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Destinations for navigation in compose
 */
@Parcelize
@Serializable
sealed class Screen : NavKey, Parcelable {

    companion object {
        const val PARCEL_KEY = "SCREEN"
    }

    @Serializable
    data object Blacklist : Screen()

    @Serializable
    data class DevProfile(val developerId: String) : Screen()

    @Serializable
    data class AppDetails(val packageName: String) : Screen()

    @Serializable
    data object Search : Screen()

    @Serializable
    data class PermissionRationale(val requiredPermissions: Set<PermissionType>) : Screen()

    @Serializable
    data object Downloads : Screen()

    @Serializable
    data object Accounts : Screen()

    @Serializable
    data object About : Screen()

    @Serializable
    data object Favourite : Screen()

    @Serializable
    data object Onboarding : Screen()

    @Serializable
    data object Spoof : Screen()

    @Serializable
    data object Dispenser : Screen()

    @Serializable
    data object Installer : Screen()

    @Serializable
    data object Installed : Screen()
}
