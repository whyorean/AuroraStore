package com.aurora.store.data.event

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Singleton

@Singleton
class FlowEvent {

    private val TAG = FlowEvent::class.java.simpleName

    private val _busEvent = MutableSharedFlow<BusEvent>(extraBufferCapacity = 1)
    val busEvent = _busEvent.asSharedFlow()

    private val _installerEvent = MutableSharedFlow<InstallerEvent>(extraBufferCapacity = 1)
    val installerEvent = _installerEvent.asSharedFlow()

    private val _authEvent = MutableSharedFlow<AuthEvent>(extraBufferCapacity = 1)
    val authEvent = _authEvent.asSharedFlow()

    fun emitEvent(event: Event) {
        when (event) {
            is InstallerEvent -> _installerEvent.tryEmit(event)
            is BusEvent -> _busEvent.tryEmit(event)
            is AuthEvent -> _authEvent.tryEmit(event)
            else -> Log.e(TAG, "Got an unhandled event")
        }
    }
}
