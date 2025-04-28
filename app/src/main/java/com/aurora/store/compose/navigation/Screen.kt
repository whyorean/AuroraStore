/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.navigation

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.aurora.store.R
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Destination (Screen) for navigation in compose
 * @param label Label of the screen
 * @param icon Optional icon for the screen; Must not be null if screen is a top-level destination
 */
@Parcelize
@Serializable
sealed class Screen(
    @StringRes val label: Int? = null,
    @DrawableRes val icon: Int? = null
) : Parcelable {

    companion object {
        const val PARCEL_KEY = "SCREEN"
    }

    @Serializable
    data object Blacklist : Screen(label = R.string.title_blacklist_manager)

    @Serializable
    data class DevProfile(val developerId: String): Screen()

    @Serializable
    data class AppDetails(val packageName: String) : Screen()

    /**
     * Child screen of [AppDetails]; Avoid navigating to this screen directly.
     */
    @Serializable
    data object DetailsMore : Screen()

    /**
     * Child screen of [AppDetails]; Avoid navigating to this screen directly.
     */
    @Serializable
    data class DetailsScreenshot(val index: Int) : Screen()

    /**
     * Child screen of [AppDetails]; Avoid navigating to this screen directly.
     */
    @Serializable
    data object DetailsExodus : Screen()

    /**
     * Child screen of [AppDetails]; Avoid navigating to this screen directly.
     */
    @Serializable
    data object DetailsReview : Screen()
}
