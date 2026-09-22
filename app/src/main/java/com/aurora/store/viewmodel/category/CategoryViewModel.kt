/*
 * SPDX-FileCopyrightText: 2021 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.viewmodel.category

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.extensions.TAG
import com.aurora.gplayapi.data.models.Category
import com.aurora.gplayapi.exceptions.GooglePlayException
import com.aurora.gplayapi.helpers.CategoryHelper
import com.aurora.gplayapi.helpers.contracts.CategoryContract
import com.aurora.store.AuroraApp
import com.aurora.store.CategoryStash
import com.aurora.store.data.event.AuthEvent
import com.aurora.store.data.model.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryHelper: CategoryHelper
) : ViewModel() {

    private var stash: CategoryStash = mutableMapOf(
        Category.Type.APPLICATION to emptyList(),
        Category.Type.GAME to emptyList()
    )

    val liveData = MutableLiveData<ViewState>()

    private fun contract(): CategoryContract = categoryHelper

    fun getCategoryList(type: Category.Type) {
        viewModelScope.launch(Dispatchers.IO) {
            val categories = getCategories(type)

            if (categories.isNotEmpty()) {
                liveData.postValue(ViewState.Success(stash))
                return@launch
            }

            liveData.postValue(ViewState.Loading)

            try {
                stash[type] = contract().getAllCategories(type)
                liveData.postValue(ViewState.Success(stash))
            } catch (exception: GooglePlayException.AuthException) {
                Log.w(TAG, "Categories fetch returned ${exception.code}, redirecting to Splash")
                AuroraApp.events.send(AuthEvent.SessionExpired())
            } catch (exception: Exception) {
                Log.e(TAG, "Failed fetching list of categories", exception)
                liveData.postValue(ViewState.Error(exception.message))
            }
        }
    }

    private fun getCategories(type: Category.Type): List<Category> = stash.getOrPut(type) {
        mutableListOf()
    }
}
