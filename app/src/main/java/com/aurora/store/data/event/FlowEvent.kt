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

    fun emitEvent(event: Event) {
        when (event) {
            is InstallerEvent -> _installerEvent.tryEmit(event)
            is BusEvent -> _busEvent.tryEmit(event)
            else -> Log.e(TAG, "Got an unhandled event")
        }
    }
}
