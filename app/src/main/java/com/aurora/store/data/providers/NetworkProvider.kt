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
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.aurora.extensions.isMAndAbove
import com.aurora.store.AuroraApp
import com.aurora.store.data.model.NetworkStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn

class NetworkProvider(context: Context) {

    private val TAG = NetworkProvider::class.java.simpleName

    private val _networkStatus = MutableStateFlow(NetworkStatus.AVAILABLE)
    val networkStatus = _networkStatus.asStateFlow()

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    init {
        networkStatus.launchIn(AuroraApp.scope)

        // Monitor network
        connectivityManager.registerNetworkCallback(getNetworkRequest(),
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    Log.d(TAG, "Network available!")
                    _networkStatus.value = NetworkStatus.AVAILABLE
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    Log.d(TAG, "Network unavailable!")
                    _networkStatus.value = NetworkStatus.LOST
                }
            })
    }

    private fun getNetworkRequest(): NetworkRequest {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

        if (isMAndAbove()) {
            networkRequest.addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }
        return  networkRequest.build()
    }
}
