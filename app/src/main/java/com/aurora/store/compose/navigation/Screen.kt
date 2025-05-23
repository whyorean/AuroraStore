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
sealed class Screen(@StringRes val label: Int, @DrawableRes val icon: Int? = null): Parcelable {

    companion object {
        const val PARCEL_KEY = "SCREEN"
    }

    @Serializable
    data object Blacklist : Screen(label = R.string.title_blacklist_manager)
}
