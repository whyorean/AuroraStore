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

package com.aurora.store.viewmodel.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.aurora.gplayapi.SearchSuggestEntry
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.helpers.SearchHelper
import com.aurora.gplayapi.helpers.contracts.SearchContract
import com.aurora.gplayapi.helpers.web.WebSearchHelper
import com.aurora.store.data.paging.GenericPagingSource.Companion.createPager
import com.aurora.store.data.providers.AuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val authProvider: AuthProvider,
    private val searchHelper: SearchHelper,
    private val webSearchHelper: WebSearchHelper
) : ViewModel() {

    private val TAG = SearchViewModel::class.java.simpleName

    private val contract: SearchContract
        get() = if (authProvider.isAnonymous) webSearchHelper else searchHelper

    private val _suggestions = MutableStateFlow<List<SearchSuggestEntry>>(emptyList())
    val suggestions = _suggestions.asStateFlow()

    private val _apps = MutableStateFlow<PagingData<App>>(PagingData.empty())
    val apps = _apps.asStateFlow()

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

        createPager { page ->
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
            _suggestions.value = contract.searchSuggestions(query).take(3)
        }
    }
}
