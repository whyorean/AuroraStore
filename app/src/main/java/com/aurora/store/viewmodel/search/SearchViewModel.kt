/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
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
import com.aurora.extensions.TAG
import com.aurora.extensions.requiresGMS
import com.aurora.gplayapi.PlayClient
import com.aurora.gplayapi.PlayException
import com.aurora.gplayapi.api.SearchSuggestion
import com.aurora.gplayapi.model.App
import com.aurora.gplayapi.model.BundleCursor
import com.aurora.gplayapi.model.StreamBundle
import com.aurora.gplayapi.web.WebPlayClient
import com.aurora.store.AuroraApp
import com.aurora.store.data.PageResult
import com.aurora.store.data.event.AuthEvent
import com.aurora.store.data.model.SearchFilter
import com.aurora.store.data.paging.GenericPagingSource.Companion.manualPager
import com.aurora.store.data.providers.AuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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

@HiltViewModel
class SearchViewModel @Inject constructor(
    val authProvider: AuthProvider,
    private val webPlayClient: WebPlayClient,
    private val playClient: PlayClient
) : ViewModel() {

    private val _suggestions = MutableStateFlow<List<SearchSuggestion>>(emptyList())
    val suggestions = _suggestions.asStateFlow()

    private val searchFilter = MutableStateFlow(SearchFilter())
    private val _apps = MutableStateFlow<PagingData<App>>(PagingData.empty())
    val apps = combine(searchFilter, _apps) { filter, pagingData ->
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
        searchFilter.value = filter
    }

    fun search(query: String) {
        var cursor: BundleCursor? = null

        fun StreamBundle.flatApps(): List<App> =
            clusters.flatMap { it.apps }.distinctBy { app -> app.packageName }

        manualPager { page ->
            val items = try {
                val bundle = when (page) {
                    1 -> webPlayClient.search.results(query)
                    else -> cursor?.let { webPlayClient.search.next(query, it) }
                }
                cursor = bundle?.next
                val apps = bundle?.flatApps() ?: emptyList()
                val exactMatch = if (page == 1) appForPackageName(query.trim()) else null
                if (exactMatch != null) {
                    listOf(exactMatch) + apps.filterNot { it.packageName == exactMatch.packageName }
                } else {
                    apps
                }
            } catch (exception: PlayException.Unauthorized) {
                Log.w(TAG, "Search returned ${exception.code}, redirecting to Splash")
                AuroraApp.events.send(AuthEvent.SessionExpired())
                emptyList()
            }
            PageResult(items)
        }.flow.distinctUntilChanged()
            .cachedIn(viewModelScope)
            .onEach { _apps.value = it }
            .launchIn(viewModelScope)
    }

    private suspend fun appForPackageName(query: String): App? {
        if (!PACKAGE_NAME.matches(query)) return null
        return try {
            playClient.apps.details(query)
        } catch (exception: PlayException.Unauthorized) {
            throw exception
        } catch (exception: Exception) {
            Log.i(TAG, "No app for package name $query", exception)
            null
        }
    }

    fun fetchSuggestions(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _suggestions.value = webPlayClient.search.suggestions(query)
                .filter { it.title.isNotBlank() }
                .take(5)
        }
    }

    companion object {
        private val PACKAGE_NAME = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$")
    }
}
