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

package com.aurora.store.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.aurora.store.data.RequestState
import com.aurora.store.data.providers.NetworkProvider
import com.aurora.store.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.lang.reflect.Modifier

abstract class BaseAndroidViewModel(application: Application) : AndroidViewModel(application),
    NetworkProvider.NetworkListener {

    private lateinit var networkListener: NetworkProvider.NetworkListener

    protected val gson: Gson = GsonBuilder()
        .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC)
        .create()

    protected var requestState: RequestState

    init {
        Log.i("${javaClass.simpleName} Created")

        requestState = RequestState.Init

        NetworkProvider.addListener(this)
    }

    abstract fun observe()

    override fun onConnected() {

    }

    override fun onDisconnected() {

    }

    override fun onReconnected() {
        redoLastNetworkTask()
    }

    private fun redoLastNetworkTask() {
        when (requestState) {
            RequestState.Pending -> {
                observe()
            }
            else -> {

            }
        }
    }

    override fun onCleared() {
        Log.i("${javaClass.simpleName} Destroyed")
        NetworkProvider.removeListener(this)
        super.onCleared()
    }
}