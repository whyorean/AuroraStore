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

package com.aurora.store.viewmodel.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.getSystemService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.aurora.extensions.isMAndAbove
import com.aurora.store.data.model.NetworkStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class NetworkViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {
    val status: MutableLiveData<NetworkStatus> = MutableLiveData(NetworkStatus.UNAVAILABLE)

    private val connectivityManager = context.getSystemService<ConnectivityManager>()
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            status.postValue(NetworkStatus.AVAILABLE)
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            status.postValue(NetworkStatus.LOST)
        }
    }

    fun register() {
        connectivityManager?.registerNetworkCallback(getNetworkRequest(), networkCallback)
    }

    fun unregister() {
        connectivityManager?.unregisterNetworkCallback(networkCallback)
    }

    private fun getNetworkRequest(): NetworkRequest {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

        if (isMAndAbove()) {
            networkRequest.addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }

        return networkRequest.build()
    }
}
