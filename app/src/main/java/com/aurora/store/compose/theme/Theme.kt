/*
 * SPDX-FileCopyrightText: 2024-2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import com.aurora.store.R
import com.aurora.store.util.Preferences

/**
 * App theme for Aurora Store based on [MaterialTheme]
 */
@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun AuroraTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val themeStyle = Preferences.getInteger(context, Preferences.PREFERENCE_THEME_STYLE)
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

    MaterialExpressiveTheme(colorScheme = colorScheme, content = content)
}
