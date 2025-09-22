/*
 * SPDX-FileCopyrightText: 2021 Rahul Kumar Patel <whyorean@gmail.com>
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.viewmodel.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import com.aurora.extensions.requiresGMS
import com.aurora.gplayapi.SearchSuggestEntry
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.helpers.SearchHelper
import com.aurora.gplayapi.helpers.contracts.SearchContract
import com.aurora.gplayapi.helpers.web.WebSearchHelper
import com.aurora.store.data.model.SearchFilter
import com.aurora.store.data.paging.GenericPagingSource.Companion.manualPager
import com.aurora.store.data.providers.AuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    val authProvider: AuthProvider,
    private val searchHelper: SearchHelper,
    private val webSearchHelper: WebSearchHelper
) : ViewModel() {

    private val TAG = SearchViewModel::class.java.simpleName

    private val contract: SearchContract
        get() = if (authProvider.isAnonymous) webSearchHelper else searchHelper

    private val _suggestions = MutableStateFlow<List<SearchSuggestEntry>>(emptyList())
    val suggestions = _suggestions.asStateFlow()

    private val _filter = MutableStateFlow(SearchFilter())
    private val _apps = MutableStateFlow<PagingData<App>>(PagingData.empty())
    val apps = combine(_filter, _apps) { filter, pagingData ->
        pagingData.filter { app ->
            when {
                filter.noAds && app.containsAds -> false
                filter.isFree && !app.isFree -> false
                filter.noGMS && app.requiresGMS() -> false
                app.rating.average < filter.minRating -> false
                app.installs < filter.minInstalls -> false
                else -> true
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), PagingData.empty())

    fun filterResults(filter: SearchFilter) {
        _filter.value = filter
    }

    fun search(query: String) {
        var nextBundleUrl: String? = null
        val nextStreamUrls = mutableSetOf<String>()

        fun Collection<StreamCluster>.flatClusters(): List<App> {
            return this.flatMap { streamCluster ->
                if (streamCluster.hasNext()) {
                    nextStreamUrls.add(streamCluster.clusterNextPageUrl)
                }
                streamCluster.clusterAppList
            }.distinctBy { app -> app.packageName }
        }

        manualPager { page ->
            try {
                when (page) {
                    1 -> contract.searchResults(query)
                        .also { nextBundleUrl = it.streamNextPageUrl }
                        .streamClusters.values
                        .flatClusters()

                    else -> {
                        when {
                            nextStreamUrls.isNotEmpty() -> {
                                nextStreamUrls.map { nextPageStreamUrl ->
                                    contract.nextStreamCluster(query, nextPageStreamUrl)
                                }.also { nextStreamUrls.clear() }.flatClusters()
                            }

                            !nextBundleUrl.isNullOrBlank() -> {
                                contract.nextStreamBundle(query, nextBundleUrl!!)
                                    .also { nextBundleUrl = it.streamNextPageUrl }
                                    .streamClusters.values
                                    .flatClusters()
                            }

                            else -> emptyList()
                        }
                        emptyList()
                    }
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to search results for $query", exception)
                emptyList()
            }
        }.flow.distinctUntilChanged()
            .cachedIn(viewModelScope)
            .onEach { _apps.value = it }
            .launchIn(viewModelScope)
    }

    fun fetchSuggestions(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _suggestions.value = contract.searchSuggestions(query)
                .filter { it.title.isNotBlank() }
                .take(3)
        }
    }
}
