/*
 * SPDX-FileCopyrightText: 2024 Aurora OSS
 * SPDX-FileCopyrightText: 2021 Rahul Kumar Patel <whyorean@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.viewmodel.subcategory

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.extensions.TAG
import com.aurora.gplayapi.data.models.StreamBundle
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.helpers.contracts.CategoryStreamContract
import com.aurora.gplayapi.helpers.contracts.StreamContract
import com.aurora.gplayapi.helpers.web.WebCategoryStreamHelper
import com.aurora.store.data.model.ViewState
import com.aurora.store.data.providers.WhitelistProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = CategoryStreamViewModel.Factory::class)
class CategoryStreamViewModel @AssistedInject constructor(
    @Assisted val browseUrl: String,
    private val webCategoryStreamHelper: WebCategoryStreamHelper,
    private val whitelistProvider: WhitelistProvider
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(browseUrl: String): CategoryStreamViewModel
    }

    private val _viewState = MutableStateFlow<ViewState>(ViewState.Loading)
    val viewState: StateFlow<ViewState> = _viewState.asStateFlow()

    private val categoryStreamContract: CategoryStreamContract
        get() = webCategoryStreamHelper

    private var streamBundle = StreamBundle.EMPTY

    init {
        fetch()
    }

    fun fetch() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!streamBundle.hasCluster() || streamBundle.hasNext()) {
                    val newBundle = if (streamBundle.streamClusters.isEmpty()) {
                        categoryStreamContract.fetch(browseUrl)
                    } else {
                        categoryStreamContract.nextStreamBundle(
                            streamBundle.id,
                            StreamContract.Category.NONE,
                            streamBundle.streamNextPageUrl
                        )
                    }

                    val filteredClusters = newBundle.streamClusters.mapValues { entry ->
                        entry.value.copy(clusterAppList = whitelistProvider.filterApps(entry.value.clusterAppList))
                    }.filterValues { it.clusterAppList.isNotEmpty() }

                    streamBundle = streamBundle.copy(
                        streamClusters = streamBundle.streamClusters + filteredClusters,
                        streamNextPageUrl = newBundle.streamNextPageUrl
                    )

                    _viewState.value = ViewState.Success(streamBundle)
                } else {
                    Log.i(TAG, "End of Bundle")
                }
            } catch (e: Exception) {
                _viewState.value = ViewState.Error(e.message)
            }
        }
    }

    fun fetchNextCluster(streamCluster: StreamCluster) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (streamCluster.hasNext()) {
                    val newCluster = categoryStreamContract.nextStreamCluster(
                        streamCluster.id,
                        streamCluster.clusterNextPageUrl
                    )
                    val mergedCluster = streamCluster.copy(
                        clusterNextPageUrl = newCluster.clusterNextPageUrl,
                        clusterAppList =
                        streamCluster.clusterAppList + whitelistProvider.filterApps(newCluster.clusterAppList)
                    )

                    val newClusters = streamBundle.streamClusters.toMutableMap().apply {
                        this[streamCluster.id] = mergedCluster
                    }
                    streamBundle = streamBundle.copy(streamClusters = newClusters)
                    _viewState.value = ViewState.Success(streamBundle)
                } else {
                    Log.i(TAG, "End of cluster")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch next cluster", e)
            }
        }
    }
}
