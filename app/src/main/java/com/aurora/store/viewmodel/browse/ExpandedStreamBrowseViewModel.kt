/*
 * SPDX-FileCopyrightText: 2024 Aurora OSS
 * SPDX-FileCopyrightText: 2021 Rahul Kumar Patel <whyorean@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.viewmodel.browse

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.aurora.extensions.TAG
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.helpers.ExpandedBrowseHelper
import com.aurora.store.data.PageResult
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

@HiltViewModel(assistedFactory = ExpandedStreamBrowseViewModel.Factory::class)
class ExpandedStreamBrowseViewModel @AssistedInject constructor(
    @Assisted val browseUrl: String,
    private val streamHelper: ExpandedBrowseHelper
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(browseUrl: String): ExpandedStreamBrowseViewModel
    }

    private val _title = MutableStateFlow("")
    val title = _title.asStateFlow()

    private val _apps = MutableStateFlow<PagingData<App>>(PagingData.empty())
    val apps = _apps.asStateFlow()

    init {
        fetchApps()
    }

    private fun fetchApps() {
        var listUrl = ""
        var nextPageUrl = ""

        manualPager { page ->
            val items = try {
                when (page) {
                    1 -> {
                        val browseResponse = streamHelper.getBrowseStreamResponse(browseUrl)
                        if (browseResponse.hasBrowseTab()) {
                            listUrl = browseResponse.browseTab.listUrl
                            val cluster = streamHelper.getExpandedBrowseClusters(listUrl)
                            _title.value = cluster.clusterTitle
                            nextPageUrl = cluster.clusterNextPageUrl
                            cluster.clusterAppList
                        } else {
                            emptyList()
                        }
                    }

                    else -> {
                        if (nextPageUrl.isNotBlank()) {
                            val cluster = streamHelper.getExpandedBrowseClusters(nextPageUrl)
                            nextPageUrl = cluster.clusterNextPageUrl
                            cluster.clusterAppList
                        } else {
                            emptyList()
                        }
                    }
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch apps for page $page", exception)
                emptyList()
            }
            PageResult(items)
        }.flow.distinctUntilChanged()
            .cachedIn(viewModelScope)
            .onEach { _apps.value = it }
            .launchIn(viewModelScope)
    }
}
