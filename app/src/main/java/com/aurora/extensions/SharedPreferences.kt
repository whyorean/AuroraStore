/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.extensions

import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Extension method to observe changes in shared preferences as a flow
 */
fun <T> SharedPreferences.observeAsStateFlow(
    key: String,
    scope: CoroutineScope,
    started: SharingStarted = SharingStarted.Eagerly,
    initial: T,
    valueProvider: () -> T
): StateFlow<T> = callbackFlow {
    val listener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == key) {
                trySend(valueProvider())
            }
        }

    // Emit the initial value
    trySend(valueProvider())

    registerOnSharedPreferenceChangeListener(listener)
    awaitClose {
        unregisterOnSharedPreferenceChangeListener(listener)
    }
}.stateIn(scope, started, initial)
