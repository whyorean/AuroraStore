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

package com.aurora.store.viewmodel.browse

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.helpers.web.WebStreamHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StreamBrowseViewModel @Inject constructor(
    private val streamHelper: WebStreamHelper
) : ViewModel() {

    private val TAG = StreamBrowseViewModel::class.java.simpleName

    val liveData: MutableLiveData<StreamCluster> = MutableLiveData()

    private lateinit var streamCluster: StreamCluster

    fun seedCluster(cluster: StreamCluster) {
        streamCluster = cluster
        liveData.postValue(streamCluster)
    }

    fun nextCluster() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (streamCluster.hasNext()) {
                    val next = streamHelper.nextStreamCluster(streamCluster.clusterNextPageUrl)

                    streamCluster = streamCluster.copy(
                        clusterNextPageUrl = next.clusterNextPageUrl,
                        clusterAppList = streamCluster.clusterAppList + next.clusterAppList
                    )

                    liveData.postValue(streamCluster)
                } else {
                    Log.i(TAG, "End of Cluster")
                    postClusterEnd()
                }
            } catch (_: Exception) {
            }
        }
    }

    fun postClusterEnd() {
        streamCluster = streamCluster.copy(
            clusterNextPageUrl = ""
        )
        liveData.postValue(streamCluster)
    }
}
