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

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.SearchSuggestEntry
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.helpers.SearchHelper
import com.aurora.gplayapi.helpers.WebSearchHelper
import com.aurora.store.data.network.HttpClient
import com.aurora.store.data.providers.AuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SearchSuggestionViewModel(application: Application) : AndroidViewModel(application) {

    private val authData: AuthData = AuthProvider
        .with(application)
        .getAuthData()

    private val webSearchHelper: WebSearchHelper = WebSearchHelper(authData)
    private val searchHelper: SearchHelper = SearchHelper(authData)
        .using(HttpClient.getPreferredClient(application))

    val liveSearchSuggestions: MutableLiveData<List<SearchSuggestEntry>> = MutableLiveData()

    fun helper(): SearchHelper {
        return if (authData.isAnonymous) {
            webSearchHelper
        } else {
            searchHelper
        }
    }

    fun observeStreamBundles(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            liveSearchSuggestions.postValue(getSearchSuggestions(query))
        }
    }

    private fun getSearchSuggestions(
        query: String
    ): List<SearchSuggestEntry> {
        return helper().searchSuggestions(query)
    }
}
