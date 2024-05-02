package com.aurora.extensions

import android.content.BroadcastReceiver
import com.aurora.store.AuroraApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun BroadcastReceiver.goAsync(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> Unit
) {
    val pendingResult = goAsync()
    AuroraApp.scope.launch(context) {
        try {
            block()
        } finally {
            pendingResult.finish()
            cancel()
        }
    }
}
