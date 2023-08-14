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
import androidx.lifecycle.viewModelScope
import com.aurora.store.data.RequestState
import com.aurora.store.data.network.HttpClient
import com.aurora.store.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.launchIn
import java.lang.reflect.Modifier

abstract class BaseAndroidViewModel(application: Application) : AndroidViewModel(application) {

    val responseCode = HttpClient.getPreferredClient().responseCode

    protected val gson: Gson = GsonBuilder()
        .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC)
        .create()

    protected var requestState: RequestState

    init {
        Log.i("${javaClass.simpleName} Created")

        requestState = RequestState.Init

        // Start collecting response code for requests
        responseCode.launchIn(viewModelScope)
    }

    abstract fun observe()

    private fun redoLastNetworkTask() {
        when (requestState) {
            RequestState.Pending -> {
                observe()
            }
            else -> {

            }
        }
    }
}
