/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.event

import android.util.Log
import com.aurora.extensions.TAG
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@Singleton
class EventFlow {

    private companion object {
        const val BUFFER_CAPACITY = 64
    }

    private val _busEvent = MutableSharedFlow<BusEvent>(extraBufferCapacity = BUFFER_CAPACITY)
    val busEvent = _busEvent.asSharedFlow()

    private val _installerEvent =
        MutableSharedFlow<InstallerEvent>(extraBufferCapacity = BUFFER_CAPACITY)
    val installerEvent = _installerEvent.asSharedFlow()

    private val _authEvent = MutableSharedFlow<AuthEvent>(extraBufferCapacity = BUFFER_CAPACITY)
    val authEvent = _authEvent.asSharedFlow()

    fun send(event: Event) {
        val emitted = when (event) {
            is InstallerEvent -> _installerEvent.tryEmit(event)
            is BusEvent -> _busEvent.tryEmit(event)
            is AuthEvent -> _authEvent.tryEmit(event)
            else -> {
                Log.e(TAG, "Got an unhandled event")
                return
            }
        }
        if (!emitted) Log.e(TAG, "Dropped event ${event::class.simpleName}, buffer full")
    }
}
