/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.extensions

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

/**
 * Text style for composable with very small text
 */
val Typography.bodyVerySmall: TextStyle
    @Composable
    get() = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp)
