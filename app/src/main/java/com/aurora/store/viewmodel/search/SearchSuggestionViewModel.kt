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

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.SearchSuggestEntry
import com.aurora.gplayapi.helpers.SearchHelper
import com.aurora.gplayapi.helpers.contracts.SearchContract
import com.aurora.gplayapi.helpers.web.WebSearchHelper
import com.aurora.gplayapi.network.IHttpClient
import com.aurora.store.data.providers.AuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchSuggestionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authProvider: AuthProvider,
    private val httpClient: IHttpClient
) : ViewModel() {

    private val _searchSuggestions = MutableStateFlow<List<SearchSuggestEntry>>(emptyList())
    val searchSuggestions = _searchSuggestions.asStateFlow()

    private val helper: SearchContract = if (authProvider.isAnonymous) {
        WebSearchHelper().using(httpClient)
    } else {
        SearchHelper(authProvider.authData!!).using(httpClient)
    }

    fun observeStreamBundles(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _searchSuggestions.value = helper.searchSuggestions(query)
        }
    }
}
