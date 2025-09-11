/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.extensions

import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.window.core.layout.WindowSizeClass
import com.aurora.store.R

/**
 * Whether the device width is compact or not
 *
 */
val WindowAdaptiveInfo.isWindowCompact: Boolean
    get() = !windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

/**
 * Returns navigation icon for adaptive screens such as extra pane
 */
val WindowAdaptiveInfo.adaptiveNavigationIcon: Painter
    @Composable
    get() = when {
        isWindowCompact -> painterResource(R.drawable.ic_arrow_back)
        else -> painterResource(R.drawable.ic_cancel)
    }
