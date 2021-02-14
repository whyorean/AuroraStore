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
import com.aurora.store.data.SingletonHolder
import com.aurora.store.util.Log
import com.novoda.merlin.Merlin

class NetworkProvider(var context: Context) {

    companion object : SingletonHolder<NetworkProvider, Context>(::NetworkProvider) {

        private var networkListeners: MutableList<NetworkListener> = mutableListOf()

        fun addListener(networkListener: NetworkListener) {
            Log.i("Network-Provider added to ${networkListener.javaClass.simpleName}")
            networkListeners.add(networkListener)
        }

        fun removeListener(networkListener: NetworkListener) {
            Log.i("Network-Provider removed from ${networkListener.javaClass.simpleName}")
            networkListeners.remove(networkListener)
        }
    }

    private var merlin: Merlin = Merlin.Builder()
        .withAllCallbacks()
        .build(context)

    private var isDisconnected = true

    fun bind() {
        merlin.bind()

        merlin.registerConnectable {
            if (isDisconnected) {
                isDisconnected = false
                onReConnected()
            } else {
                onConnected()
            }
        }

        merlin.registerDisconnectable {
            isDisconnected = true
            onDisconnected()
        }
    }

    fun unbind() {
        networkListeners.clear()
        merlin.unbind()
        Log.i("Network-Provider destroyed")
    }

    private fun onConnected() {
        Log.i("Network-Provider connected")
        isDisconnected = false
        networkListeners.forEach {
            it.onConnected()
        }
    }

    private fun onReConnected() {
        Log.i("Network-Provider reconnected")
        networkListeners.forEach {
            it.onReconnected()
        }
    }

    private fun onDisconnected() {
        Log.e("Network-Provider disconnected")
        networkListeners.forEach {
            it.onDisconnected()
        }
    }

    interface NetworkListener {
        fun onConnected()
        fun onDisconnected()
        fun onReconnected()
    }
}
