/*
 * SPDX-FileCopyrightText: 2021 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
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
import com.aurora.gplayapi.exceptions.GooglePlayException
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.gplayapi.helpers.StreamHelper
import com.aurora.gplayapi.helpers.contracts.StreamContract
import com.aurora.store.AuroraApp
import com.aurora.store.data.event.AuthEvent
import com.aurora.store.data.model.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltViewModel
class DevProfileViewModel @Inject constructor(
    private val appDetailsHelper: AppDetailsHelper,
    private val streamHelper: StreamHelper
) : ViewModel() {

    val liveData: MutableLiveData<ViewState> = MutableLiveData()
    var devStream: DevStream = DevStream.EMPTY
    var streamBundle: StreamBundle = StreamBundle.EMPTY

    lateinit var type: StreamContract.Type
    lateinit var category: StreamContract.Category

    fun getStreamBundle(devId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                devStream = appDetailsHelper.getDeveloperStream(devId)
                streamBundle = devStream.streamBundle
                liveData.postValue(ViewState.Success(devStream))
            } catch (e: GooglePlayException.AuthException) {
                Log.w(TAG, "Developer stream fetch returned ${e.code}, redirecting to Splash")
                AuroraApp.events.send(AuthEvent.SessionExpired())
            } catch (e: Exception) {
                liveData.postValue(ViewState.Error(e.message))
            }
        }
    }

    fun observeCluster(streamCluster: StreamCluster) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (streamCluster.hasNext()) {
                    val newCluster = streamHelper.getNextStreamCluster(
                        streamCluster.id,
                        streamCluster.clusterNextPageUrl
                    )
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
