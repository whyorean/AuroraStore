/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import coil3.compose.LocalAsyncImagePreviewHandler
import com.aurora.store.compose.theme.AuroraTheme

/**
 * Template for previewing composable with default theme and remote image handling
 */
@Composable
fun PreviewTemplate(content : @Composable () -> Unit) {
    AuroraTheme {
        CompositionLocalProvider(LocalAsyncImagePreviewHandler provides coilPreviewProvider) {
            content()
        }
    }
}
