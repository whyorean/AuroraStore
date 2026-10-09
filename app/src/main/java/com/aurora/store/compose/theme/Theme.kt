/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-FileCopyrightText: 2024-2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.theme

import android.content.SharedPreferences
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.aurora.store.util.Preferences

/**
 * App theme for Aurora Store based on [MaterialExpressiveTheme]
 */
@Composable
fun AuroraTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var themeStyle by remember {
        mutableIntStateOf(Preferences.getInteger(context, Preferences.PREFERENCE_THEME_STYLE))
    }
    var dynamicColor by remember {
        mutableStateOf(
            Preferences.getBoolean(
                context,
                Preferences.PREFERENCE_DYNAMIC_COLORS,
                Preferences.dynamicColorsDefault
            )
        )
    }
    DisposableEffect(context) {
        val prefs = Preferences.getPrefs(context)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                Preferences.PREFERENCE_THEME_STYLE ->
                    themeStyle = Preferences.getInteger(context, key)

                Preferences.PREFERENCE_DYNAMIC_COLORS ->
                    dynamicColor = Preferences.getBoolean(
                        context,
                        key,
                        Preferences.dynamicColorsDefault
                    )
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val useDynamicColor = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val lightScheme =
        if (useDynamicColor) dynamicLightColorScheme(context) else BrandLightColorScheme
    val darkScheme =
        if (useDynamicColor) dynamicDarkColorScheme(context) else BrandDarkColorScheme
    val systemDarkTheme = isSystemInDarkTheme()
    val darkTheme = when (themeStyle) {
        1 -> false
        2 -> true
        else -> systemDarkTheme
    }
    val colorScheme = if (darkTheme) darkScheme else lightScheme

    /**
     * Keep the status/navigation bar icon appearance in sync with the system light/dark mode.
     *
     * The bars themselves are kept transparent and edge-to-edge by [androidx.activity.enableEdgeToEdge]
     * (called once in the host activity); we deliberately don't touch `window.statusBarColor` /
     * `navigationBarColor` here since those setters are deprecated no-ops on Android 15+ and would
     * otherwise strip the system contrast scrim. Re-applying the appearance keeps the icons correct
     * when Android or the app's saved theme preference switches between light and dark modes.
     */
    val view = LocalView.current
    val activity = LocalActivity.current
    if (!view.isInEditMode) {
        SideEffect {
            val currentActivity = activity ?: return@SideEffect
            WindowCompat
                .getInsetsController(currentActivity.window, view)
                .apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
        }
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        content = content
    )
}
