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

package com.aurora.store.viewmodel.topchart

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.helpers.contracts.TopChartsContract
import com.aurora.gplayapi.helpers.web.WebTopChartsHelper
import com.aurora.gplayapi.network.IHttpClient
import com.aurora.store.TopChartStash
import com.aurora.store.data.model.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak") // false positive, see https://github.com/google/dagger/issues/3253
class TopChartViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: IHttpClient
): ViewModel() {

    private val webTopChartsHelper: TopChartsContract = WebTopChartsHelper()
        .using(httpClient)

    private var stash: TopChartStash = mutableMapOf()

    val liveData: MutableLiveData<ViewState> = MutableLiveData()

    private fun contract(): TopChartsContract {
        return webTopChartsHelper
    }

    fun getStreamCluster(type: TopChartsContract.Type, chart: TopChartsContract.Chart) {
        viewModelScope.launch(Dispatchers.IO) {
            if (targetCluster(type, chart).clusterAppList.isNotEmpty()) {
                liveData.postValue(ViewState.Success(stash))
            }

            try {
                val cluster = contract().getCluster(type.value, chart.value)
                updateCluster(type, chart, cluster)
                liveData.postValue(ViewState.Success(stash))
            } catch (_: Exception) {
            }
        }
    }

    fun nextCluster(type: TopChartsContract.Type, chart: TopChartsContract.Chart) {
        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                try {
                    val target = targetCluster(type, chart)
                    if (target.hasNext()) {
                        val newCluster = contract().getNextStreamCluster(
                            target.clusterNextPageUrl
                        )

                        updateCluster(type, chart, newCluster)

                        liveData.postValue(ViewState.Success(stash))
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun updateCluster(
        type: TopChartsContract.Type,
        chart: TopChartsContract.Chart,
        newCluster: StreamCluster
    ) {
        targetCluster(type, chart).apply {
            clusterAppList.addAll(newCluster.clusterAppList)
            clusterNextPageUrl = newCluster.clusterNextPageUrl
        }
    }

    private fun targetCluster(
        type: TopChartsContract.Type,
        chart: TopChartsContract.Chart
    ): StreamCluster {
        val cluster = stash
            .getOrPut(type) { mutableMapOf() }
            .getOrPut(chart) { StreamCluster() }
        return cluster
    }
}
