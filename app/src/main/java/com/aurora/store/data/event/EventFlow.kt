package com.aurora.store.data.event

import android.util.Log
import com.aurora.extensions.TAG
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Singleton

@Singleton
class EventFlow {

    private val _busEvent = MutableSharedFlow<BusEvent>(extraBufferCapacity = 1)
    val busEvent = _busEvent.asSharedFlow()

    private val _installerEvent = MutableSharedFlow<InstallerEvent>(extraBufferCapacity = 1)
    val installerEvent = _installerEvent.asSharedFlow()

    private val _authEvent = MutableSharedFlow<AuthEvent>(extraBufferCapacity = 1)
    val authEvent = _authEvent.asSharedFlow()

    fun send(event: Event) {
        when (event) {
            is InstallerEvent -> _installerEvent.tryEmit(event)
            is BusEvent -> _busEvent.tryEmit(event)
            is AuthEvent -> _authEvent.tryEmit(event)
            else -> Log.e(TAG, "Got an unhandled event")
        }
    }
}
