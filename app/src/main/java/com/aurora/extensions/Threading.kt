/*
 * SPDX-FileCopyrightText: 2021 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.extensions

import android.os.Handler
import android.os.Looper

fun runOnUiThread(action: () -> Unit) {
    when {
        isMainThread() -> action.invoke()
        else -> Handler(Looper.getMainLooper()).post(Runnable(action))
    }
}

private fun isMainThread() = Looper.myLooper() == Looper.getMainLooper()
