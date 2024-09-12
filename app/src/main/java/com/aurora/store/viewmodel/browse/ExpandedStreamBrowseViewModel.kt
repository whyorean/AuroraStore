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

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.helpers.ExpandedBrowseHelper
import com.aurora.gplayapi.network.IHttpClient
import com.aurora.store.data.providers.AuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak") // false positive, see https://github.com/google/dagger/issues/3253
class ExpandedStreamBrowseViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authProvider: AuthProvider,
    private val httpClient: IHttpClient
) : ViewModel() {

    private val TAG = ExpandedStreamBrowseViewModel::class.java.simpleName

    private val streamHelper: ExpandedBrowseHelper = ExpandedBrowseHelper(authProvider.authData!!)
        .using(httpClient)

    val liveData: MutableLiveData<StreamCluster> = MutableLiveData()
    var streamCluster: StreamCluster = StreamCluster()

    fun getInitialCluster(expandedStreamUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                try {
                    val browseResponse = streamHelper.getBrowseStreamResponse(expandedStreamUrl)
                    if (browseResponse.hasBrowseTab()) {
                        streamCluster =
                            streamHelper.getExpandedBrowseClusters(browseResponse.browseTab.listUrl)
                        liveData.postValue(streamCluster)
                    } else {
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    fun next() {
        Log.e(TAG, "NEXT CALED")
        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                try {
                    val newCluster = streamHelper.getExpandedBrowseClusters(
                        streamCluster.clusterNextPageUrl
                    )

                    streamCluster.apply {
                        clusterAppList.addAll(newCluster.clusterAppList)
                        clusterNextPageUrl = newCluster.clusterNextPageUrl
                    }

                    liveData.postValue(streamCluster)

                    if (!streamCluster.hasNext()) {
                        Log.i(TAG, "End of Bundle")
                    }
                } catch (exception: Exception) {
                    Log.e(TAG, "Failed to fetch next stream", exception)
                }
            }
        }
    }
}
