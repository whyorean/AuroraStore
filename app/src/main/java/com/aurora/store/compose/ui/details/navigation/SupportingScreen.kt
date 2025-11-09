/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details.navigation

import android.os.Parcelable
import androidx.navigation3.runtime.NavKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Supporting destinations for app detail's screen
 */
@Parcelize
@Serializable
sealed class SupportingScreen : NavKey, Parcelable {

    @Serializable
    data object Suggestions : SupportingScreen()
}
