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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.aurora.extensions.TAG
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.helpers.web.WebStreamHelper
import com.aurora.store.data.paging.GenericPagingSource.Companion.manualPager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@HiltViewModel(assistedFactory = StreamBrowseViewModel.Factory::class)
class StreamBrowseViewModel @AssistedInject constructor(
    @Assisted private val streamCluster: StreamCluster,
    private val streamHelper: WebStreamHelper
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(streamCluster: StreamCluster): StreamBrowseViewModel
    }

    private val _apps = MutableStateFlow<PagingData<App>>(PagingData.empty())
    val apps = _apps.asStateFlow()

    init {
        fetchApps()
    }

    private fun fetchApps() {
        var nextPageUrl: String = streamCluster.clusterNextPageUrl

        manualPager { page ->
            try {
                when (page) {
                    1 -> streamCluster.clusterAppList

                    else -> {
                        if (nextPageUrl.isNotBlank()) {
                            streamHelper.nextStreamCluster(nextPageUrl).also {
                                nextPageUrl = it.clusterNextPageUrl
                            }.clusterAppList
                        } else {
                            emptyList()
                        }
                    }
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch apps for $page: $nextPageUrl", exception)
                emptyList()
            }
        }.flow.distinctUntilChanged()
            .cachedIn(viewModelScope)
            .onEach { _apps.value = it }
            .launchIn(viewModelScope)
    }
}
