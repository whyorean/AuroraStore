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

package com.aurora.store.viewmodel.subcategory

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.data.models.StreamBundle
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.helpers.contracts.CategoryStreamContract
import com.aurora.gplayapi.helpers.contracts.StreamContract
import com.aurora.gplayapi.helpers.web.WebCategoryStreamHelper
import com.aurora.store.data.model.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

@HiltViewModel
class CategoryStreamViewModel @Inject constructor(
    private val webCategoryStreamHelper: WebCategoryStreamHelper
) : ViewModel() {

    private val TAG = CategoryStreamViewModel::class.java.simpleName

    val liveData: MutableLiveData<ViewState> = MutableLiveData()

    private var stash: MutableMap<String, StreamBundle> = mutableMapOf()

    private val categoryStreamContract: CategoryStreamContract
        get() = webCategoryStreamHelper

    fun getStreamBundle(browseUrl: String) {
        liveData.postValue(ViewState.Loading)
        observe(browseUrl)
    }

    fun observe(browseUrl: String) {
        liveData.postValue(ViewState.Loading)
        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                val bundle = targetBundle(browseUrl)
                if (bundle.streamClusters.isNotEmpty()) {
                    liveData.postValue(ViewState.Success(stash))
                }

                try {
                    if (!bundle.hasCluster() || bundle.hasNext()) {
                        //Fetch new stream bundle
                        val newBundle = if (bundle.streamClusters.isEmpty()) {
                            categoryStreamContract.fetch(browseUrl)
                        } else {
                            categoryStreamContract.nextStreamBundle(
                                StreamContract.Category.NONE,
                                bundle.streamNextPageUrl
                            )
                        }

                        //Update old bundle
                        val mergedBundle = bundle.copy(
                            streamClusters = bundle.streamClusters + newBundle.streamClusters,
                            streamNextPageUrl = newBundle.streamNextPageUrl
                        )
                        stash[browseUrl] = mergedBundle

                        //Post updated to UI
                        liveData.postValue(ViewState.Success(stash))
                    } else {
                        Log.i(TAG, "End of Bundle")
                    }
                } catch (e: Exception) {
                    liveData.postValue(ViewState.Error(e.message))
                }
            }
        }
    }

    fun observeCluster(browseUrl: String, streamCluster: StreamCluster) {
        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                try {
                    if (streamCluster.hasNext()) {
                        val newCluster = categoryStreamContract.nextStreamCluster(
                            streamCluster.clusterNextPageUrl
                        )
                        updateCluster(browseUrl, streamCluster.id, newCluster)
                        liveData.postValue(ViewState.Success(stash))
                    } else {
                        Log.i(TAG, "End of cluster")
                    }
                } catch (e: Exception) {
                    liveData.postValue(ViewState.Error(e.message))
                }
            }
        }
    }

    private fun updateCluster(browseUrl: String, clusterID: Int, newCluster: StreamCluster) {
        val bundle = targetBundle(browseUrl)
        bundle.streamClusters[clusterID]?.let { oldCluster ->
            val mergedCluster = oldCluster.copy(
                clusterNextPageUrl = newCluster.clusterNextPageUrl,
                clusterAppList = oldCluster.clusterAppList + newCluster.clusterAppList
            )
            val newStreamClusters = bundle.streamClusters.toMutableMap().also {
                it.remove(clusterID)
                it[clusterID] = mergedCluster
            }

            stash.put(browseUrl, bundle.copy(streamClusters = newStreamClusters))
        }
    }

    private fun targetBundle(browseUrl: String): StreamBundle {
        val streamBundle = stash.getOrPut(browseUrl) { StreamBundle() }
        return streamBundle
    }
}
