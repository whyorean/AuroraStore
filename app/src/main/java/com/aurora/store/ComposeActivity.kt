/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store

import android.content.Intent
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
import com.aurora.store.data.receiver.MigrationReceiver
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.Preferences
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ComposeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        MigrationReceiver.runMigrationsIfRequired(this)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        intent.setExtrasClassLoader(Screen::class.java.classLoader)

        val startDestination = resolveStartDestination()

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

    private fun resolveStartDestination(): Screen {
        // Parcel-based navigation (e.g. from NotificationUtil)
        IntentCompat.getParcelableExtra(intent, Screen.PARCEL_KEY, Screen::class.java)
            ?.let { return it }

        // Deep links via ACTION_VIEW
        if (intent.action == Intent.ACTION_VIEW) {
            val data = intent.data
            val path = data?.path.orEmpty()
            val id = data?.getQueryParameter("id")
            return when {
                id != null && path.contains("/apps/dev") -> Screen.DevProfile(id)
                id != null -> Screen.AppDetails(id)
                else -> defaultStart()
            }
        }

        // SEND / SHOW_APP_INFO — getPackageName() handles both
        intent.getPackageName()?.let { return Screen.AppDetails(it) }

        return defaultStart()
    }

    private fun defaultStart(): Screen = when {
        !Preferences.getBoolean(this, Preferences.PREFERENCE_INTRO) -> Screen.Onboarding
        else -> Screen.Splash
    }
}
