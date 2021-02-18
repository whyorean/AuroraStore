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

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.data.models.Category
import com.aurora.gplayapi.helpers.CategoryHelper
import com.aurora.store.data.RequestState
import com.aurora.store.data.network.HttpClient
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.viewmodel.BaseAndroidViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class BaseCategoryViewModel(application: Application) : BaseAndroidViewModel(application) {

    private val authData: AuthData = AuthProvider
        .with(application)
        .getAuthData()

    private val streamHelper: CategoryHelper = CategoryHelper(authData)
        .using(HttpClient.getPreferredClient())

    val liveData: MutableLiveData<List<Category>> = MutableLiveData()

    lateinit var type: Category.Type

    override fun observe() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                liveData.postValue(streamHelper.getAllCategoriesList(type))
                requestState = RequestState.Complete
            } catch (e: Exception) {
                requestState = RequestState.Pending
            }
        }
    }
}