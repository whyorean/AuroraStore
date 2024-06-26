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

package com.aurora.store.viewmodel.homestream

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.data.models.StreamBundle
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.helpers.StreamHelper
import com.aurora.store.data.model.ViewState
import com.aurora.gplayapi.helpers.contracts.StreamContract
import com.aurora.gplayapi.helpers.web.WebStreamHelper
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
@SuppressLint("StaticFieldLeak") // false positive, see https://github.com/google/dagger/issues/3253
class StreamViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authProvider: AuthProvider
) : ViewModel() {

    private var streamHelper: StreamContract = StreamHelper(authProvider.authData)
        .using(HttpClient.getPreferredClient(context))

    private var webStreamHelper: StreamContract = WebStreamHelper()
        .using(HttpClient.getPreferredClient(context))

    val liveData: MutableLiveData<ViewState> = MutableLiveData()

    private var streamBundleMap: MutableMap<StreamContract.Category, StreamBundle> = mutableMapOf(
        StreamContract.Category.APPLICATION to StreamBundle(),
        StreamContract.Category.GAME to StreamBundle()
    )

    fun contract(): StreamContract {
        return if (authProvider.isAnonymous) {
            webStreamHelper
        } else {
            streamHelper
        }
    }

    fun getStreamBundle(category: StreamContract.Category, type: StreamContract.Type) {
        liveData.postValue(ViewState.Loading)
        observe(category, type)
    }

    fun observe(category: StreamContract.Category, type: StreamContract.Type) {
        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                if (targetBundle(category).streamClusters.isNotEmpty()) {
                    liveData.postValue(ViewState.Success(targetBundle(category)))
                }

                try {
                    if (!targetBundle(category).hasCluster() || targetBundle(category).hasNext()) {

                        //Fetch new stream bundle
                        val newBundle = if (targetBundle(category).streamClusters.isEmpty()) {
                            contract().fetch(type, category)
                        } else {
                            contract().nextStreamBundle(
                                category,
                                targetBundle((category)).streamNextPageUrl
                            )
                        }

                        //Update old bundle
                        targetBundle(category).apply {
                            streamClusters.putAll(newBundle.streamClusters)
                            streamNextPageUrl = newBundle.streamNextPageUrl
                        }

                        //Post updated to UI
                        liveData.postValue(ViewState.Success(targetBundle(category)))
                    } else {
                        Log.i("End of Bundle")
                    }
                } catch (e: Exception) {
                    liveData.postValue(ViewState.Error(e.message))
                }
            }
        }
    }

    fun observeCluster(category: StreamContract.Category, streamCluster: StreamCluster) {
        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                try {
                    if (streamCluster.hasNext()) {
                        val newCluster =
                            contract().nextStreamCluster(streamCluster.clusterNextPageUrl)
                        updateCluster(category, streamCluster.id, newCluster)
                        liveData.postValue(ViewState.Success(targetBundle(category)))
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
        category: StreamContract.Category,
        clusterID: Int,
        newCluster: StreamCluster
    ) {
        targetBundle(category).streamClusters[clusterID]?.apply {
            clusterAppList.addAll(newCluster.clusterAppList)
            clusterNextPageUrl = newCluster.clusterNextPageUrl
        }
    }

    private fun targetBundle(category: StreamContract.Category): StreamBundle {
        val streamBundle = streamBundleMap[category]

        return streamBundle!!
    }
}
