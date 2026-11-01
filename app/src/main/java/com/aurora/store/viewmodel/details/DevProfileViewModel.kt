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
import com.aurora.gplayapi.PlayClient
import com.aurora.gplayapi.PlayException
import com.aurora.gplayapi.model.DeveloperStream
import com.aurora.gplayapi.model.StreamBundle
import com.aurora.gplayapi.model.StreamCluster
import com.aurora.store.AuroraApp
import com.aurora.store.data.event.AuthEvent
import com.aurora.store.data.model.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltViewModel
class DevProfileViewModel @Inject constructor(
    private val playClient: PlayClient
) : ViewModel() {

    val liveData: MutableLiveData<ViewState> = MutableLiveData()
    var devStream: DeveloperStream = DeveloperStream.EMPTY
    var streamBundle: StreamBundle = StreamBundle.EMPTY

    fun getStreamBundle(devId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                devStream = playClient.apps.developerStream(devId)
                streamBundle = devStream.streamBundle
                liveData.postValue(ViewState.Success(devStream))
            } catch (e: PlayException.Unauthorized) {
                Log.w(TAG, "Developer stream fetch returned ${e.code}, redirecting to Splash")
                AuroraApp.events.send(AuthEvent.SessionExpired())
            } catch (e: Exception) {
                liveData.postValue(ViewState.Error(e.message))
            }
        }
    }

    fun observeCluster(cluster: StreamCluster) {
        val cursor = cluster.next
        if (cursor == null) {
            Log.i(TAG, "End of cluster")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val newCluster = playClient.streams.next(cursor)
                updateCluster(cluster, newCluster)
                devStream = devStream.copy(streamBundle = streamBundle)
                liveData.postValue(ViewState.Success(devStream))
            } catch (e: Exception) {
                liveData.postValue(ViewState.Error(e.message))
            }
        }
    }

    private fun updateCluster(oldCluster: StreamCluster, newCluster: StreamCluster) {
        val mergedCluster = oldCluster.copy(
            apps = oldCluster.apps + newCluster.apps,
            next = newCluster.next
        )

        streamBundle = streamBundle.copy(
            clusters = streamBundle.clusters.map {
                if (it.id ==
                    oldCluster.id
                ) {
                    mergedCluster
                } else {
                    it
                }
            }
        )
    }
}
