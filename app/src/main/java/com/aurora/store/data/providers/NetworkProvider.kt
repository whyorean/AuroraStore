/*
 * SPDX-FileCopyrightText: 2021 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.providers

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import androidx.core.content.getSystemService
import com.aurora.extensions.isNAndAbove
import com.aurora.store.data.model.NetworkStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * A simple provider with a flow to observe internet connectivity changes
 */
@Singleton
class NetworkProvider @Inject constructor(@ApplicationContext private val context: Context) {

    private val connectivityManager = context.getSystemService<ConnectivityManager>()!!

    val status: Flow<NetworkStatus>
        get() = callbackFlow {
            val networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    trySend(NetworkStatus.AVAILABLE).isSuccess
                }

                override fun onLost(network: Network) {
                    trySend(NetworkStatus.UNAVAILABLE).isSuccess
                }
            }

            if (isNAndAbove) {
                connectivityManager.registerDefaultNetworkCallback(networkCallback)
            } else {
                connectivityManager.registerNetworkCallback(
                    NetworkRequest.Builder().build(),
                    networkCallback
                )
            }

            awaitClose { connectivityManager.unregisterNetworkCallback(networkCallback) }
        }.distinctUntilChanged()
}
