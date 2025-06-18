/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.extensions

import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.window.core.layout.WindowWidthSizeClass
import com.aurora.store.R

/**
 * Returns navigation icon for adaptive screens such as extra pane
 */
val WindowAdaptiveInfo.adaptiveNavigationIcon: Painter
    @Composable
    get() = when (windowSizeClass.windowWidthSizeClass) {
        WindowWidthSizeClass.COMPACT -> painterResource(R.drawable.ic_arrow_back)
        else -> painterResource(R.drawable.ic_cancel)
    }
