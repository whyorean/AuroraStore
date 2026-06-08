/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.tv

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import com.aurora.store.data.providers.AccountProvider
import com.aurora.store.data.receiver.MigrationReceiver
import com.aurora.store.tv.navigation.Navigation
import com.aurora.store.tv.navigation.resolveStartDestination
import com.aurora.store.tv.theme.Theme
import com.aurora.store.util.Preferences
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        MigrationReceiver.runMigrationsIfRequired(this)
        super.onCreate(savedInstanceState)

        val introCompleted = Preferences.getBoolean(this, Preferences.PREFERENCE_INTRO)
        val startDestination = resolveStartDestination(
            introCompleted = introCompleted,
            isLoggedIn = AccountProvider.isLoggedIn(this)
        )

        setContent {
            Theme {
                Navigation(startDestination = startDestination)
            }
        }
    }
}
