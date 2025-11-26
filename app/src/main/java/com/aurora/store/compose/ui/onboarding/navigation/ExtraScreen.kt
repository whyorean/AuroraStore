/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.onboarding.navigation

import android.os.Parcelable
import androidx.navigation3.runtime.NavKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Extra destinations for onboarding
 *
 * All of these destinations are child destinations of onboarding screen are shown inside it.
 */
@Parcelize
@Serializable
sealed class ExtraScreen : NavKey, Parcelable {

    @Serializable
    data object Welcome : ExtraScreen()

    @Serializable
    data object Permissions : ExtraScreen()
}
