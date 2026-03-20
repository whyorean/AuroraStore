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
 * Extra destinations for app detail's screen
 *
 * All of these destinations require and show information related to an app and thus aren't part of
 * the main navigation display class.
 */
@Parcelize
@Serializable
sealed class ExtraScreen : NavKey, Parcelable {

    @Serializable
    data object More : ExtraScreen()

    @Serializable
    data class Screenshot(val index: Int) : ExtraScreen()

    @Serializable
    data object Exodus : ExtraScreen()

    @Serializable
    data object Review : ExtraScreen()

    @Serializable
    data object Permission : ExtraScreen()

    @Serializable
    data object ManualDownload : ExtraScreen()

    @Serializable
    data object MicroG : ExtraScreen()
}
