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

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.extensions.TAG
import com.aurora.gplayapi.data.models.StreamBundle
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.data.models.details.DevStream
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.gplayapi.helpers.StreamHelper
import com.aurora.gplayapi.helpers.contracts.StreamContract
import com.aurora.store.data.model.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

@HiltViewModel
class DevProfileViewModel @Inject constructor(
    private val appDetailsHelper: AppDetailsHelper,
    private val streamHelper: StreamHelper
) : ViewModel() {

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
                    }
                } catch (e: Exception) {
                    liveData.postValue(ViewState.Error(e.message))
                }
            }
        }
    }

    private fun updateCluster(newCluster: StreamCluster) {
        streamBundle.streamClusters[newCluster.id]?.let { oldCluster ->
            val mergedCluster = oldCluster.copy(
                clusterNextPageUrl = newCluster.clusterNextPageUrl,
                clusterAppList = oldCluster.clusterAppList + newCluster.clusterAppList
            )

            val newStreamClusters = streamBundle.streamClusters.toMutableMap().apply {
                this[newCluster.id] = mergedCluster
            }

            streamBundle = streamBundle.copy(streamClusters = newStreamClusters)
        }
    }
}
