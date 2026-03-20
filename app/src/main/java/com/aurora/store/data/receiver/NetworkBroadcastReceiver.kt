/*
 * SPDX-FileCopyrightText: 2025 Aurora OSS
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import androidx.core.content.getSystemService

/**
 * Broadcast receiver for network status for API 21 & 22
 * @param callback Callback when the network status changes
 */
@Suppress("DEPRECATION")
class NetworkBroadcastReceiver(private val callback: (Boolean) -> Unit) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val connectivityManager = context?.getSystemService<ConnectivityManager>()
        val networkInfo = connectivityManager?.activeNetworkInfo
        val isConnected = networkInfo?.isConnectedOrConnecting == true

        callback(isConnected)
    }
}
