/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.tv.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor as Material3LocalContentColor
import androidx.compose.material3.MaterialTheme as Material3Theme
import androidx.compose.material3.darkColorScheme as material3DarkColorScheme
import androidx.compose.material3.lightColorScheme as material3LightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme

/**
 * App theme for the TV flavor.
 *
 * TV screens mix two design systems: our own chrome uses Compose-for-TV (`androidx.tv.material3`),
 * while the reused app-details sections come from the phone UI and use Material3
 * (`androidx.compose.material3`). The two libraries have *separate* `MaterialTheme` and
 * `LocalContentColor` composition locals, so both must be themed or one set of components renders
 * with wrong/black colors. We therefore:
 *  - provide the TV color scheme + a TV [Surface] (themed background and TV content color), and
 *  - provide the Material3 color scheme + explicitly set the Material3 content color to onSurface
 *    (the TV [Surface] only sets the TV content color; the Material3 one would otherwise default to
 *    black and be invisible in dark mode).
 *
 * TVs are predominantly dark-themed; we follow the system setting but default to dark.
 */
@Composable
fun Theme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    val tvColorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
    val material3ColorScheme =
        if (darkTheme) material3DarkColorScheme() else material3LightColorScheme()

    MaterialTheme(colorScheme = tvColorScheme) {
        Material3Theme(colorScheme = material3ColorScheme) {
            Surface(modifier = Modifier.fillMaxSize()) {
                CompositionLocalProvider(
                    Material3LocalContentColor provides material3ColorScheme.onSurface
                ) {
                    content()
                }
            }
        }
    }
}
