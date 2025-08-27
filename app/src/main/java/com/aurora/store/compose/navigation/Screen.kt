/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.navigation

import android.os.Parcelable
import androidx.navigation3.runtime.NavKey
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
    data class DevProfile(val developerId: String): Screen()

    @Serializable
    data class AppDetails(val packageName: String) : Screen()

    @Serializable
    data object Search : Screen()
}
