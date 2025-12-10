/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.viewmodel.dispenser

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_DISPENSER_URLS
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlin.collections.toMutableSet

@HiltViewModel
class DispenserViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val sharedPreferences = Preferences.getPrefs(context)

    private var _dispensers: Set<String>
        set(value) = sharedPreferences.edit { putStringSet(PREFERENCE_DISPENSER_URLS, value) }
        get() = sharedPreferences.getStringSet(PREFERENCE_DISPENSER_URLS, emptySet()) ?: emptySet()

    val dispensers: StateFlow<Set<String>>
        get() {
            return callbackFlow {
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
                    if (changedKey == PREFERENCE_DISPENSER_URLS) trySend(_dispensers)
                }

                trySend(_dispensers)

                sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
                awaitClose {
                    sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
                }
            }.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
        }

    fun addDispenser(url: String) {
        _dispensers = _dispensers.toMutableSet().apply {
            add(url)
        }
    }

    fun removeDispenser(url: String) {
        _dispensers = _dispensers.toMutableSet().apply {
            remove(url)
        }
    }
}
