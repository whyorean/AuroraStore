/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.content.IntentCompat
import com.aurora.extensions.getPackageName
import com.aurora.store.compose.composition.LocalUI
import com.aurora.store.compose.composition.UI
import com.aurora.store.compose.navigation.NavDisplay
import com.aurora.store.compose.navigation.Screen
import com.aurora.store.compose.theme.AuroraTheme
import com.aurora.store.util.PackageUtil
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ComposeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Ensure the classloader is set for the Screen parcelable
        intent.setExtrasClassLoader(Screen::class.java.classLoader)

        var startDestination = IntentCompat.getParcelableExtra(
            intent,
            Screen.PARCEL_KEY,
            Screen::class.java
        ) ?: Screen.Onboarding

        // If the intent contains a package name for app details,
        // Override the start destination to be the app details screen for that package.
        val packageName = intent.getPackageName()
        if (packageName != null) {
            startDestination = Screen.AppDetails(packageName)
        }

        val localUI = when {
            PackageUtil.isTv(this) -> UI.TV
            else -> UI.DEFAULT
        }

        setContent {
            CompositionLocalProvider(LocalUI provides localUI) {
                AuroraTheme {
                    NavDisplay(startDestination = startDestination)
                }
            }
        }
    }
}
