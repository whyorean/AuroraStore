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
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

@HiltViewModel(assistedFactory = CategoryStreamViewModel.Factory::class)
class CategoryStreamViewModel @AssistedInject constructor(
    @Assisted val browseUrl: String,
    private val webCategoryStreamHelper: WebCategoryStreamHelper
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(browseUrl: String): CategoryStreamViewModel
    }

    // SharedFlow (instead of StateFlow) because StreamBundle/StreamCluster override equals
    // to compare only id, which is preserved by copy(). StateFlow would conflate every update
    // and break paginated scroll loading.
    private val _viewState = MutableSharedFlow<ViewState>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val viewState: SharedFlow<ViewState> = _viewState.asSharedFlow()

    private val categoryStreamContract: CategoryStreamContract
        get() = webCategoryStreamHelper

    private var streamBundle = StreamBundle()

    init {
        fetchNextPage()
    }

    fun fetchNextPage() {
        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                if (streamBundle.streamClusters.isNotEmpty()) {
                    _viewState.tryEmit(ViewState.Success(streamBundle))
                }

                try {
                    if (!streamBundle.hasCluster() || streamBundle.hasNext()) {
                        val newBundle = if (streamBundle.streamClusters.isEmpty()) {
                            categoryStreamContract.fetch(browseUrl)
                        } else {
                            categoryStreamContract.nextStreamBundle(
                                StreamContract.Category.NONE,
                                streamBundle.streamNextPageUrl
                            )
                        }

                        streamBundle = streamBundle.copy(
                            streamClusters = streamBundle.streamClusters + newBundle.streamClusters,
                            streamNextPageUrl = newBundle.streamNextPageUrl
                        )

                        _viewState.tryEmit(ViewState.Success(streamBundle))
                    } else {
                        Log.i(TAG, "End of Bundle")
                    }
                } catch (e: Exception) {
                    _viewState.tryEmit(ViewState.Error(e.message))
                }
            }
        }
    }

    fun fetchNextCluster(streamCluster: StreamCluster) {
        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                try {
                    if (streamCluster.hasNext()) {
                        val newCluster = categoryStreamContract.nextStreamCluster(
                            streamCluster.clusterNextPageUrl
                        )
                        val mergedCluster = streamCluster.copy(
                            clusterNextPageUrl = newCluster.clusterNextPageUrl,
                            clusterAppList =
                            streamCluster.clusterAppList + newCluster.clusterAppList
                        )

                        val newClusters = streamBundle.streamClusters.toMutableMap().apply {
                            this[streamCluster.id] = mergedCluster
                        }
                        streamBundle = streamBundle.copy(streamClusters = newClusters)
                        _viewState.tryEmit(ViewState.Success(streamBundle))
                    } else {
                        Log.i(TAG, "End of cluster")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to fetch next cluster", e)
                }
            }
        }
    }
}
