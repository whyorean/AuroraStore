/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-FileCopyrightText: 2021 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.viewmodel.browse

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.aurora.extensions.TAG
import com.aurora.gplayapi.PlayException
import com.aurora.gplayapi.model.App
import com.aurora.gplayapi.model.ClusterCursor
import com.aurora.gplayapi.model.StreamCluster
import com.aurora.gplayapi.web.WebPlayClient
import com.aurora.store.AuroraApp
import com.aurora.store.data.PageResult
import com.aurora.store.data.event.AuthEvent
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

/**
 * Pages a cluster whose header was clicked, always over the web lane
 * ([WebPlayClient.streams]) - ported unchanged from v3's own `StreamBrowseViewModel`, which
 * always used `WebStreamHelper` regardless of where [streamCluster] itself came from.
 * `Destination.StreamBrowse` is reachable from a home/category stream header (web lane,
 * consistent) and from a developer-profile stream header
 * ([com.aurora.gplayapi.api.AppApi.developerStream], authenticated lane) - in the latter
 * case [streamCluster]'s own [StreamCluster.next] cursor was minted by the authenticated
 * lane, and handing its `nextPageUrl` to the web lane's `qnKhOb` RPC here is a pre-existing
 * cross-lane mismatch, not one this migration introduces. See the task report.
 */
@HiltViewModel(assistedFactory = StreamBrowseViewModel.Factory::class)
class StreamBrowseViewModel @AssistedInject constructor(
    @Assisted private val streamCluster: StreamCluster,
    private val webPlayClient: WebPlayClient
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(streamCluster: StreamCluster): StreamBrowseViewModel
    }

    private val _apps = MutableStateFlow<PagingData<App>>(PagingData.empty())
    val apps = _apps.asStateFlow()

    init {
        fetch()
    }

    fun fetch() {
        var cursor: ClusterCursor? = streamCluster.next

        manualPager { page ->
            val items = try {
                when (page) {
                    1 -> streamCluster.apps

                    else -> {
                        val nextCursor = cursor
                        if (nextCursor != null) {
                            val cluster = webPlayClient.streams.next(nextCursor)
                            cursor = cluster.next
                            cluster.apps
                        } else {
                            emptyList()
                        }
                    }
                }
            } catch (exception: PlayException.Unauthorized) {
                Log.w(TAG, "Stream returned ${exception.code}, redirecting to Splash")
                AuroraApp.events.send(AuthEvent.SessionExpired())
                emptyList()
            }
            PageResult(items)
        }.flow.distinctUntilChanged()
            .cachedIn(viewModelScope)
            .onEach { _apps.value = it }
            .launchIn(viewModelScope)
    }
}
