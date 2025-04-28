/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.extensions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.window.core.layout.WindowWidthSizeClass

/**
 * Returns navigation icon for adaptive screens such as extra pane
 */
val WindowAdaptiveInfo.adaptiveNavigationIcon: ImageVector
    get() = when (windowSizeClass.windowWidthSizeClass) {
        WindowWidthSizeClass.COMPACT -> Icons.AutoMirrored.Filled.ArrowBack
        else -> Icons.Default.Close
    }
