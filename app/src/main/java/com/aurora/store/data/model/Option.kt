/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.model

import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import com.aurora.store.compose.navigation.Screen

abstract class Option(
    @StringRes open val title: Int,
    @DrawableRes open val icon: Int
)

data class ViewOption(
    override val title: Int,
    override val icon: Int,
    @IdRes val destinationID: Int
) : Option(title, icon)

data class ComposeOption(
    override val title: Int,
    override val icon: Int,
    val screen: Screen
) : Option(title, icon)
