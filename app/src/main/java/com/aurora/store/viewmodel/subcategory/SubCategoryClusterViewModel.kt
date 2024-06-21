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

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.data.models.StreamBundle
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.helpers.CategoryHelper
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
class SubCategoryClusterViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    var contract: StreamContract = WebStreamHelper()
        .using(HttpClient.getPreferredClient(context))

    val liveData: MutableLiveData<ViewState> = MutableLiveData()
    var streamBundle: StreamBundle = StreamBundle()

    private lateinit var homeUrl: String
    private lateinit var type: StreamContract.Type
    private lateinit var category: StreamContract.Category

    init {
        liveData.postValue(ViewState.Loading)
    }

    private fun getCategoryStreamBundle(
        nextPageUrl: String
    ): StreamBundle {
        return if (streamBundle.streamClusters.isEmpty())
            contract.fetch(type, category)
        else
            contract.nextStreamBundle(category, nextPageUrl)
    }

    fun observeCategory(homeUrl: String) {
        this.homeUrl = homeUrl

        val rawCategory = homeUrl.split("/").last()

        type = StreamContract.Type.HOME
        category = StreamContract.Category.NONE.apply {
            value = rawCategory
        }

        observe()
    }

    fun observe() {
        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                try {
                    if (!streamBundle.hasCluster() || streamBundle.hasNext()) {
                        //Fetch new stream bundle
                        val newBundle = getCategoryStreamBundle(
                            streamBundle.streamNextPageUrl
                        )

                        //Update old bundle
                        streamBundle.apply {
                            streamClusters.putAll(newBundle.streamClusters)
                            streamNextPageUrl = newBundle.streamNextPageUrl
                        }

                        //Post updated to UI
                        liveData.postValue(ViewState.Success(streamBundle))
                    } else {
                        Log.i("End of Bundle")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
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
                        val newCluster =
                            contract.nextStreamCluster(streamCluster.clusterNextPageUrl)
                        updateCluster(newCluster)
                        liveData.postValue(ViewState.Success(streamBundle))
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

    private fun updateCluster(newCluster: StreamCluster) {
        streamBundle.streamClusters[newCluster.id]?.apply {
            clusterAppList.addAll(newCluster.clusterAppList)
            clusterNextPageUrl = newCluster.clusterNextPageUrl
        }
    }
}
