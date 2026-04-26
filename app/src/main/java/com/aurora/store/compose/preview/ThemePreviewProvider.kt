/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.PreviewWrapperProvider
import coil3.compose.LocalAsyncImagePreviewHandler
import com.aurora.store.compose.theme.AuroraTheme

/**
 * Preview provider for default theme and remote image handling
 */
class ThemePreviewProvider : PreviewWrapperProvider {

    @Composable
    override fun Wrap(content: @Composable (() -> Unit)) {
        AuroraTheme {
            CompositionLocalProvider(
                value = LocalAsyncImagePreviewHandler provides coilPreviewProvider,
                content = content
            )
        }
    }
}
