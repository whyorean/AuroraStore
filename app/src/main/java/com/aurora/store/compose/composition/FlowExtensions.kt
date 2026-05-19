/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

/**
 * Collects a [Flow] as [State] starting from [initial], always triggering recomposition on
 * every emission regardless of structural equality, by using [neverEqualPolicy].
 *
 * Useful for non-state flows (e.g. [kotlinx.coroutines.flow.SharedFlow]) and for flows whose
 * values have broken `equals` implementations that would otherwise be conflated upstream.
 */
@Composable
internal fun <T> Flow<T>.collectForced(initial: T): State<T> {
    val lifecycleOwner = LocalLifecycleOwner.current
    val state: MutableState<T> = remember(this) { mutableStateOf(initial, neverEqualPolicy()) }
    LaunchedEffect(this, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            collect { state.value = it }
        }
    }
    return state
}

/**
 * Observes a [LiveData] as [State], always triggering recomposition on every emission
 * regardless of structural equality, by using [neverEqualPolicy].
 */
@Suppress("UNCHECKED_CAST")
@Composable
internal fun <T> LiveData<T>.observeForced(): State<T?> {
    val lifecycleOwner = LocalLifecycleOwner.current
    val state: MutableState<T?> = remember { mutableStateOf(value, neverEqualPolicy()) }
    DisposableEffect(this, lifecycleOwner) {
        val observer = Observer<T> { state.value = it }
        observe(lifecycleOwner, observer)
        onDispose { removeObserver(observer) }
    }
    return state
}
