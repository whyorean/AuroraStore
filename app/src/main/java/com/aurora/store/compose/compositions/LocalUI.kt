/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.compositions

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Supported UI styles for different types of Android OS
 */
enum class UI {

    /**
     * Targets Phone, Foldable, Tablets, Desktop
     */
    DEFAULT,

    /**
     * Targets TV
     */
    TV
}

/**
 * CompositionLocal to provide information on which UI style should be used
 */
val LocalUI = staticCompositionLocalOf { UI.DEFAULT }
