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
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import com.aurora.store.data.model.NetworkStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * A simple provider with a flow to observe internet connectivity changes
 */
@Singleton
class NetworkProvider @Inject constructor(@ApplicationContext private val context: Context) {

    private val connectivityManager = context.getSystemService<ConnectivityManager>()!!

    @get:RequiresApi(Build.VERSION_CODES.N)
    val status: Flow<NetworkStatus>
        get() {
            return callbackFlow {
                val networkCallback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        super.onAvailable(network)
                        launch { send(NetworkStatus.AVAILABLE) }
                    }

                    override fun onLost(network: Network) {
                        super.onLost(network)
                        launch { send(NetworkStatus.UNAVAILABLE) }
                    }
                }

                connectivityManager.registerDefaultNetworkCallback(networkCallback)
                awaitClose { connectivityManager.unregisterNetworkCallback(networkCallback) }
            }.distinctUntilChanged()
        }
}
