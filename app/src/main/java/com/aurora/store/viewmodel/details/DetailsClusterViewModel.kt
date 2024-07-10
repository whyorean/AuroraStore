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

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.data.models.StreamBundle
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.gplayapi.helpers.StreamHelper
import com.aurora.gplayapi.helpers.contracts.StreamContract
import com.aurora.store.AppStreamStash
import com.aurora.store.data.model.ViewState
import com.aurora.store.data.network.HttpClient
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.util.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

@HiltViewModel
class DetailsClusterViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    authProvider: AuthProvider
) : ViewModel() {

    private var appDetailsHelper = AppDetailsHelper(authProvider.authData)
        .using(HttpClient.getPreferredClient(context))
    private var streamHelper = StreamHelper(authProvider.authData)


    val liveData: MutableLiveData<ViewState> = MutableLiveData()
    private val stash: AppStreamStash = mutableMapOf()

    lateinit var type: StreamContract.Type
    lateinit var category: StreamContract.Category

    fun getStreamBundle(streamUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                val bundle = targetBundle(streamUrl)
                if (bundle.streamClusters.isNotEmpty()) {
                    liveData.postValue(ViewState.Success(stash))
                }

                try {
                    if (!bundle.hasCluster() || bundle.hasNext()) {
                        val newBundle = appDetailsHelper.getDetailsStream(streamUrl)

                        bundle.apply {
                            streamClusters.putAll(newBundle.streamClusters)
                            streamNextPageUrl = newBundle.streamNextPageUrl
                        }

                        liveData.postValue(ViewState.Success(stash))
                    }
                } catch (e: Exception) {
                    liveData.postValue(ViewState.Error(e.message))
                }
            }
        }
    }

    fun observeCluster(url: String, streamCluster: StreamCluster) {
        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                try {
                    if (streamCluster.hasNext()) {
                        val newCluster =
                            streamHelper.getNextStreamCluster(streamCluster.clusterNextPageUrl)
                        updateCluster(url, streamCluster.id, newCluster)
                        liveData.postValue(ViewState.Success(stash))
                    } else {
                        Log.i("End of cluster")
                        streamCluster.clusterNextPageUrl = String()
                    }
                } catch (e: Exception) {
                    liveData.postValue(ViewState.Error(e.message))
                }
            }
        }
    }

    private fun updateCluster(
        url: String,
        clusterID: Int,
        newCluster: StreamCluster
    ) {
        targetBundle(url).streamClusters[clusterID]?.apply {
            clusterAppList.addAll(newCluster.clusterAppList)
            clusterNextPageUrl = newCluster.clusterNextPageUrl
        }
    }

    private fun targetBundle(url: String): StreamBundle {
        val streamBundle = stash.getOrPut(url) {
            StreamBundle()
        }

        return streamBundle
    }
}
