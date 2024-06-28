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

package com.aurora.store.viewmodel.category

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.data.models.Category
import com.aurora.gplayapi.helpers.CategoryHelper
import com.aurora.gplayapi.helpers.contracts.CategoryContract
import com.aurora.gplayapi.helpers.web.WebCategoryHelper
import com.aurora.store.CategoryStash
import com.aurora.store.data.ViewState
import com.aurora.store.data.network.HttpClient
import com.aurora.store.data.providers.AuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak") // false positive, see https://github.com/google/dagger/issues/3253
class CategoryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authProvider: AuthProvider
) : ViewModel() {
    private val TAG = CategoryViewModel::class.java.simpleName

    private val categoryHelper: CategoryHelper = CategoryHelper(authProvider.authData)
        .using(HttpClient.getPreferredClient(context))

    private val webCategoryHelper: CategoryContract = WebCategoryHelper()
        .using(HttpClient.getPreferredClient(context))

    private var stash: CategoryStash = mutableMapOf(
        Category.Type.APPLICATION to emptyList(),
        Category.Type.GAME to emptyList()
    )

    val liveData = MutableLiveData<ViewState>()

    private fun contract(): CategoryContract {
        return if (authProvider.isAnonymous) {
            webCategoryHelper
        } else {
            categoryHelper
        }
    }

    fun getCategoryList(type: Category.Type) {
        viewModelScope.launch(Dispatchers.IO) {
            val categories = getCategories(type)

            if (categories.isNotEmpty()) {
                liveData.postValue(ViewState.Success(stash))
            }

            try {
                stash[type] = contract().getAllCategoriesList(type)
                liveData.postValue(ViewState.Success(stash))
            } catch (exception: Exception) {
                Log.e(TAG, "Failed fetching list of categories", exception)
            }
        }
    }

    private fun getCategories(type: Category.Type): List<Category> {
        return stash.getOrPut(type) {
            mutableListOf()
        }
    }
}
