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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.aurora.extensions.flushAndAdd
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.SearchBundle
import com.aurora.gplayapi.helpers.SearchHelper
import com.aurora.gplayapi.helpers.contracts.SearchContract
import com.aurora.gplayapi.helpers.web.WebSearchHelper
import com.aurora.store.data.paging.GenericPagingSource.Companion.createPager
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.data.providers.FilterProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

@HiltViewModel
class SearchResultViewModel @Inject constructor(
    val filterProvider: FilterProvider,
    private val authProvider: AuthProvider,
    private val searchHelper: SearchHelper,
    private val webSearchHelper: WebSearchHelper
) : ViewModel() {

    private val TAG = SearchResultViewModel::class.java.simpleName

    val liveData: MutableLiveData<SearchBundle> = MutableLiveData()

    private var searchBundle: SearchBundle = SearchBundle()

    private val helper: SearchContract
        get() = if (authProvider.isAnonymous) webSearchHelper else searchHelper

    private var subBundles = mutableSetOf<SearchBundle.SubBundle>()
    private val _apps = MutableStateFlow<PagingData<App>>(PagingData.empty())
    val apps = _apps.asStateFlow()

    fun search(query: String) {
        viewModelScope.launch {
            searchFlow(query)
                .distinctUntilChanged()
                .cachedIn(viewModelScope)
                .collect { _apps.value = it }
        }
    }

    private fun searchFlow(query: String): Flow<PagingData<App>> {
        return createPager { page ->
            when (page) {
                1 -> {
                    val result = helper.searchResults(query)
                    subBundles = result.subBundles
                    result.appList
                }

                else -> {
                    try {
                        val result = helper.next(subBundles)
                        if (result.appList.isNotEmpty()) {
                            subBundles = result.subBundles
                            result.appList
                        } else {
                            emptyList()
                        }
                    } catch (exception: Exception) {
                        Log.d(TAG, "Failed to get next bundle", exception)
                        emptyList()
                    }
                }
            }
        }.flow
    }

    fun observeSearchResults(query: String) {
        //Clear old results
        searchBundle.subBundles.clear()
        searchBundle.appList.clear()
        //Fetch new results
        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                try {
                    searchBundle = oldSearch(query)
                    liveData.postValue(searchBundle)
                } catch (e: Exception) {

                }
            }
        }
    }

    private fun oldSearch(query: String): SearchBundle {
        return helper.searchResults(query)
    }

    @Synchronized
    fun next(nextSubBundleSet: MutableSet<SearchBundle.SubBundle>) {
        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                try {
                    if (nextSubBundleSet.isNotEmpty()) {
                        val newSearchBundle = helper.next(nextSubBundleSet)
                        if (newSearchBundle.appList.isNotEmpty()) {
                            searchBundle.apply {
                                subBundles.flushAndAdd(newSearchBundle.subBundles)
                                appList.addAll(newSearchBundle.appList)
                            }

                            liveData.postValue(searchBundle)
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Failed to get next bundle", e)
                }
            }
        }
    }
}
