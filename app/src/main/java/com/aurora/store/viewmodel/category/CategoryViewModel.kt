/*
 * SPDX-FileCopyrightText: 2021 Rahul Kumar Patel <whyorean@gmail.com>
 * SPDX-FileCopyrightText: 2026 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.viewmodel.category

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.extensions.TAG
import com.aurora.gplayapi.data.models.Category
import com.aurora.gplayapi.helpers.CategoryHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = CategoryViewModel.Factory::class)
class CategoryViewModel @AssistedInject constructor(
    @Assisted private val type: Category.Type,
    private val categoryHelper: CategoryHelper
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(type: Category.Type): CategoryViewModel
    }

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories = _categories.asStateFlow()

    init {
        getCategoryList()
    }

    private fun getCategoryList() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _categories.value = categoryHelper.getAllCategories(type)
            } catch (exception: Exception) {
                Log.e(TAG, "Failed fetching list of categories", exception)
            }
        }
    }
}
