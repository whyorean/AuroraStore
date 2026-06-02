/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Whether the active [MaterialTheme] is dark.
 *
 * Derived from the resolved color scheme rather than [androidx.compose.foundation.isSystemInDarkTheme]
 * so it stays correct when the user forces a light/dark theme that differs from the system setting.
 */
@Composable
@ReadOnlyComposable
private fun isAppInDarkTheme(): Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f

/**
 * Amber used to flag warnings/caveats. Lightened in dark theme for adequate contrast.
 */
val warningColor: Color
    @Composable @ReadOnlyComposable
    get() = if (isAppInDarkTheme()) Color(0xFFFFB74D) else Color(0xFFFF7600)

/**
 * Green used to flag positive/success states. Lightened in dark theme for adequate contrast.
 */
val successColor: Color
    @Composable @ReadOnlyComposable
    get() = if (isAppInDarkTheme()) Color(0xFF5BD27A) else Color(0xFF1B8738)

val colorGreen: Color
    @Composable @ReadOnlyComposable
    get() = if (isAppInDarkTheme()) Color(0xFF81C784) else Color(0xFF388E3C)

val colorRed: Color
    @Composable @ReadOnlyComposable
    get() = if (isAppInDarkTheme()) Color(0xFFE57373) else Color(0xFFD32F2F)

/**
 * Brand color schemes seeded from Aurora's accent (#6C63FF), used on devices that don't support
 * dynamic color (Android 11 and below) so the full palette stays on-brand instead of falling back
 * to Material's default purple baseline.
 */
val BrandLightColorScheme = lightColorScheme(
    primary = Color(0xFF6C63FF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE4DFFF),
    onPrimaryContainer = Color(0xFF1A0066),
    secondary = Color(0xFF5C5D72),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE1E0F9),
    onSecondaryContainer = Color(0xFF191A2C),
    tertiary = Color(0xFF78536B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8EE),
    onTertiaryContainer = Color(0xFF2E1126),
    background = Color(0xFFFCF8FF),
    onBackground = Color(0xFF1B1B21),
    surface = Color(0xFFFCF8FF),
    onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFE4E1EC),
    onSurfaceVariant = Color(0xFF47464F),
    outline = Color(0xFF777680),
    outlineVariant = Color(0xFFC8C5D0),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

val BrandDarkColorScheme = darkColorScheme(
    primary = Color(0xFFC8BFFF),
    onPrimary = Color(0xFF31149C),
    primaryContainer = Color(0xFF534BD6),
    onPrimaryContainer = Color(0xFFE4DFFF),
    secondary = Color(0xFFC5C4DD),
    onSecondary = Color(0xFF2E2F42),
    secondaryContainer = Color(0xFF444559),
    onSecondaryContainer = Color(0xFFE1E0F9),
    tertiary = Color(0xFFE8B9D5),
    onTertiary = Color(0xFF46263B),
    tertiaryContainer = Color(0xFF5E3C52),
    onTertiaryContainer = Color(0xFFFFD8EE),
    background = Color(0xFF131318),
    onBackground = Color(0xFFE4E1E9),
    surface = Color(0xFF131318),
    onSurface = Color(0xFFE4E1E9),
    surfaceVariant = Color(0xFF47464F),
    onSurfaceVariant = Color(0xFFC8C5D0),
    outline = Color(0xFF918F9A),
    outlineVariant = Color(0xFF47464F),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)
