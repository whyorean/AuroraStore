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

package com.aurora.store.viewmodel.all

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.helpers.ClusterHelper
import com.aurora.store.data.RequestState
import com.aurora.store.data.network.HttpClient
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.viewmodel.BaseAndroidViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class LibraryAppsViewModel(application: Application) : BaseAndroidViewModel(application) {

    private val authData: AuthData = AuthProvider.with(application).getAuthData()
    private val clusterHelper: ClusterHelper =
        ClusterHelper(authData).using(HttpClient.getPreferredClient())

    val liveData: MutableLiveData<StreamCluster> = MutableLiveData()
    var streamCluster: StreamCluster = StreamCluster()

    init {
        observe()
    }

    override fun observe() {
        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                try {
                    when {
                        streamCluster.clusterAppList.isEmpty() -> {
                            val newCluster =
                                clusterHelper.getCluster(ClusterHelper.Type.MY_APPS_LIBRARY)
                            updateCluster(newCluster)
                            liveData.postValue(streamCluster)
                        }
                        streamCluster.hasNext() -> {
                            val newCluster = clusterHelper.next(streamCluster.clusterNextPageUrl)
                            updateCluster(newCluster)
                            liveData.postValue(streamCluster)
                        }
                        else -> {
                            requestState = RequestState.Complete
                        }
                    }
                } catch (e: Exception) {
                    requestState = RequestState.Pending
                }
            }
        }
    }

    private fun updateCluster(newCluster: StreamCluster) {
        streamCluster.apply {
            clusterAppList.addAll(newCluster.clusterAppList)
            clusterNextPageUrl = newCluster.clusterNextPageUrl
        }
    }
}