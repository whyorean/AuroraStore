/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.search

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset

/** Shared full-screen slide used by the main-screen overlay and direct Search destinations. */
internal val searchPanelMotionSpec = tween<IntOffset>(
    durationMillis = 280,
    easing = LinearOutSlowInEasing
)
