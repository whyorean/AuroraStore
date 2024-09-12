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

package com.aurora.store.viewmodel.details

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.data.models.StreamBundle
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.data.models.details.DevStream
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.gplayapi.helpers.StreamHelper
import com.aurora.gplayapi.helpers.contracts.StreamContract
import com.aurora.gplayapi.network.IHttpClient
import com.aurora.store.data.model.ViewState
import com.aurora.store.data.providers.AuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak") // false positive, see https://github.com/google/dagger/issues/3253
class DevProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authProvider: AuthProvider,
    private val httpClient: IHttpClient
) : ViewModel() {

    private val TAG = DevProfileViewModel::class.java.simpleName

    private var appDetailsHelper = AppDetailsHelper(authProvider.authData!!)
        .using(httpClient)
    private var streamHelper = StreamHelper(authProvider.authData!!)

    val liveData: MutableLiveData<ViewState> = MutableLiveData()
    var devStream:DevStream = DevStream()
    var streamBundle: StreamBundle = StreamBundle()

    lateinit var type: StreamContract.Type
    lateinit var category: StreamContract.Category

    fun getStreamBundle(devId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                try {
                    devStream = appDetailsHelper.getDeveloperStream(devId)
                    streamBundle = devStream.streamBundle
                    liveData.postValue(ViewState.Success(devStream))
                } catch (e: Exception) {
                    liveData.postValue(ViewState.Error(e.message))
                }
            }
        }
    }

    fun observeCluster(streamCluster: StreamCluster) {
        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                try {
                    if (streamCluster.hasNext()) {
                        val newCluster = streamHelper.getNextStreamCluster(streamCluster.clusterNextPageUrl)
                        updateCluster(newCluster)
                        devStream = devStream.copy(streamBundle = streamBundle)
                        liveData.postValue(ViewState.Success(devStream))
                    } else {
                        Log.i(TAG, "End of cluster")
                        streamCluster.clusterNextPageUrl = String()
                    }
                } catch (e: Exception) {
                    liveData.postValue(ViewState.Error(e.message))
                }
            }
        }
    }

    private fun updateCluster(newCluster: StreamCluster) {
        streamBundle.streamClusters[newCluster.id]?.apply {
            clusterAppList.addAll(newCluster.clusterAppList)
            clusterNextPageUrl = newCluster.clusterNextPageUrl
        }
    }
}
