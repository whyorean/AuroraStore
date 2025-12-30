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

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.extensions.TAG
import com.aurora.gplayapi.data.models.Category
import com.aurora.gplayapi.helpers.CategoryHelper
import com.aurora.gplayapi.helpers.contracts.CategoryContract
import com.aurora.store.CategoryStash
import com.aurora.store.data.model.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryHelper: CategoryHelper
) : ViewModel() {

    private var stash: CategoryStash = mutableMapOf(
        Category.Type.APPLICATION to emptyList(),
        Category.Type.GAME to emptyList()
    )

    val liveData = MutableLiveData<ViewState>()

    private fun contract(): CategoryContract {
        return categoryHelper
    }

    fun getCategoryList(type: Category.Type) {
        viewModelScope.launch(Dispatchers.IO) {
            val categories = getCategories(type)

            if (categories.isNotEmpty()) {
                liveData.postValue(ViewState.Success(stash))
                return@launch
            }

            try {
                stash[type] = contract().getAllCategories(type)
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
