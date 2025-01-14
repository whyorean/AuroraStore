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
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.SearchBundle
import com.aurora.gplayapi.helpers.SearchHelper
import com.aurora.gplayapi.helpers.contracts.SearchContract
import com.aurora.gplayapi.helpers.web.WebSearchHelper
import com.aurora.store.data.paging.GenericPagingSource.Companion.createPager
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.data.providers.FilterProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchResultViewModel @Inject constructor(
    val filterProvider: FilterProvider,
    authProvider: AuthProvider,
    searchHelper: SearchHelper,
    webSearchHelper: WebSearchHelper
) : ViewModel() {

    private val TAG = SearchResultViewModel::class.java.simpleName

    private val helper: SearchContract = if (authProvider.isAnonymous) {
        webSearchHelper
    } else {
        searchHelper
    }

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

    private fun shouldFilter(app: App): Boolean {
        val filter = filterProvider.getSavedFilter()
        return app.displayName.isNotEmpty() &&
                (filter.paidApps || app.isFree) &&
                (filter.appsWithAds || !app.containsAds) &&
                (filter.gsfDependentApps || app.dependencies.dependentPackages.isEmpty()) &&
                (filter.rating <= 0 || app.rating.average >= filter.rating) &&
                (filter.downloads <= 0 || app.installs >= filter.downloads)
    }
}
