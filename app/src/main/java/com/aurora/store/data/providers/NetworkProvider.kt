/*
 * Aurora Store
 *  Copyright (C) 2021, Rahul Kumar Patel <whyorean@gmail.com>
 *
 *  Aurora Store is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Aurora Store is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.aurora.store.data.providers

import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import androidx.core.content.getSystemService
import com.aurora.extensions.isMAndAbove
import com.aurora.extensions.isNAndAbove
import com.aurora.store.data.model.NetworkStatus
import com.aurora.store.data.receiver.NetworkBroadcastReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A simple provider with a flow to observe internet connectivity changes
 */
@Singleton
class NetworkProvider @Inject constructor(@ApplicationContext private val context: Context) {

    private val connectivityManager = context.getSystemService<ConnectivityManager>()!!

    val status: Flow<NetworkStatus>
        get() = callbackFlow {
            if (isMAndAbove) {
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
            } else {
                val receiver = NetworkBroadcastReceiver { isConnected ->
                    val status = if (isConnected) {
                        NetworkStatus.AVAILABLE
                    } else {
                        NetworkStatus.UNAVAILABLE
                    }
                    trySend(status).isSuccess
                }

                @Suppress("DEPRECATION")
                context.registerReceiver(
                    receiver,
                    IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
                )

                awaitClose { context.unregisterReceiver(receiver) }
            }
        }.distinctUntilChanged()
}
