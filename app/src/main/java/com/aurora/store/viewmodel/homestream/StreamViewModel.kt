/*
 * SPDX-FileCopyrightText: 2021 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.viewmodel.homestream

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.extensions.TAG
import com.aurora.gplayapi.data.models.StreamBundle
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.helpers.contracts.StreamContract
import com.aurora.gplayapi.helpers.web.WebStreamHelper
import com.aurora.store.HomeStash
import com.aurora.store.data.model.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@HiltViewModel
class StreamViewModel @Inject constructor(
    private val webStreamHelper: WebStreamHelper
) : ViewModel() {

    val liveData: MutableLiveData<ViewState> = MutableLiveData()

    private val stash: HomeStash = mutableMapOf()

    private val streamContract: StreamContract
        get() = webStreamHelper

    // Mutex to protect stash access for thread safety
    private val stashMutex = Mutex()

    fun getStreamBundle(category: StreamContract.Category, type: StreamContract.Type) {
        liveData.postValue(ViewState.Loading)
        observe(category, type)
    }

    fun observe(category: StreamContract.Category, type: StreamContract.Type) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                stashMutex.withLock {
                    val bundle = targetBundle(category)

                    // Post existing data if any clusters exist
                    if (bundle.hasCluster()) {
                        liveData.postValue(ViewState.Success(stash.toMap()))
                    }

                    if (!bundle.hasCluster() || bundle.hasNext()) {
                        // Fetch new stream bundle
                        val newBundle = if (bundle.hasCluster()) {
                            streamContract.nextStreamBundle(
                                bundle.id,
                                category,
                                bundle.streamNextPageUrl
                            )
                        } else {
                            streamContract.fetch(type, category)
                        }

                        val mergedBundle = bundle.copy(
                            streamClusters = bundle.streamClusters + newBundle.streamClusters,
                            streamNextPageUrl = newBundle.streamNextPageUrl
                        )
                        stash[category] = mergedBundle

                        // Post updated to UI
                        liveData.postValue(ViewState.Success(stash.toMap()))
                    } else {
                        Log.i(TAG, "End of Bundle")
                    }
                }
            } catch (e: Exception) {
                liveData.postValue(ViewState.Error(e.message))
            }
        }
    }

    fun observeCluster(category: StreamContract.Category, streamCluster: StreamCluster) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (streamCluster.hasNext()) {
                    val newCluster = streamContract.nextStreamCluster(
                        streamCluster.id,
                        streamCluster.clusterNextPageUrl
                    )

                    stashMutex.withLock {
                        updateCluster(category, streamCluster.id, newCluster)
                    }

                    liveData.postValue(ViewState.Success(stash.toMap()))
                } else {
                    stashMutex.withLock {
                        postClusterEnd(category, streamCluster.id)
                    }

                    liveData.postValue(ViewState.Success(stash.toMap()))
                }
            } catch (e: Exception) {
                liveData.postValue(ViewState.Error(e.message))
            }
        }
    }

    private fun updateCluster(
        category: StreamContract.Category,
        clusterID: Int,
        newCluster: StreamCluster
    ) {
        val bundle = stash[category] ?: return
        val oldCluster = bundle.streamClusters[clusterID] ?: return

        val mergedCluster = oldCluster.copy(
            clusterNextPageUrl = newCluster.clusterNextPageUrl,
            clusterAppList = (oldCluster.clusterAppList + newCluster.clusterAppList)
                .distinctBy { it.packageName }
        )

        val updatedClusters = bundle.streamClusters.toMutableMap().apply {
            this[clusterID] = mergedCluster
        }

        stash[category] = bundle.copy(streamClusters = updatedClusters)
    }

    private fun postClusterEnd(category: StreamContract.Category, clusterID: Int) {
        val bundle = stash[category] ?: return
        val oldCluster = bundle.streamClusters[clusterID] ?: return

        val updatedCluster = oldCluster.copy(clusterNextPageUrl = "")
        val updatedClusters = bundle.streamClusters.toMutableMap().apply {
            this[clusterID] = updatedCluster
        }

        stash[category] = bundle.copy(streamClusters = updatedClusters)
    }

    private fun targetBundle(category: StreamContract.Category): StreamBundle =
        stash.getOrPut(category) {
            StreamBundle(id = category.value.hashCode())
        }
}
