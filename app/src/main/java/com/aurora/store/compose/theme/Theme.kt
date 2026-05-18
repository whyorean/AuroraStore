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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.core.view.WindowCompat
import com.aurora.store.R
import com.aurora.store.util.Preferences

/**
 * App theme for Aurora Store based on [MaterialTheme]
 */
@Composable
fun AuroraTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current

    var themeStyle by remember {
        mutableIntStateOf(Preferences.getInteger(context, Preferences.PREFERENCE_THEME_STYLE))
    }
    DisposableEffect(Unit) {
        val prefs = Preferences.getPrefs(context)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == Preferences.PREFERENCE_THEME_STYLE) {
                themeStyle = Preferences.getInteger(context, Preferences.PREFERENCE_THEME_STYLE)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val isDynamicColorSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val lightScheme = if (isDynamicColorSupported) {
        dynamicLightColorScheme(context)
    } else {
        lightColorScheme(primary = colorResource(id = R.color.colorAccent))
    }

    val darkScheme = if (isDynamicColorSupported) {
        dynamicDarkColorScheme(context)
    } else {
        darkColorScheme(primary = colorResource(id = R.color.colorAccent))
    }

    val colorScheme = when (themeStyle) {
        1 -> lightScheme
        2 -> darkScheme
        else -> if (isSystemInDarkTheme()) darkScheme else lightScheme
    }

    val darkTheme = when (themeStyle) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }

    /**
     * Side effect to update the system bars to be transparent and match the theme's light/dark mode.
     * This is necessary on OEM devices that don't properly support dynamic theming and may have issues with light/dark status bar icons.
     */
    val view = LocalView.current
    val activity = LocalActivity.current
    if (!view.isInEditMode) {
        SideEffect {
            val currentActivity = activity ?: return@SideEffect
            val window = currentActivity.window

            // Transparent system bars
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT

            // Control icon colors explicitly
            WindowCompat
                .getInsetsController(window, view)
                .apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
        }
    }

    MaterialExpressiveTheme(colorScheme = colorScheme, content = content)
}
