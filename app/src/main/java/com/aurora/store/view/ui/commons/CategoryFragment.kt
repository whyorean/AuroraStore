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

package com.aurora.store.view.ui.commons

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.aurora.Constants
import com.aurora.gplayapi.data.models.Category
import com.aurora.store.CategoryStash
import com.aurora.store.data.model.ViewState
import com.aurora.store.data.model.ViewState.Empty.getDataAs
import com.aurora.store.databinding.FragmentGenericRecyclerBinding
import com.aurora.store.view.epoxy.views.CategoryViewModel_
import com.aurora.store.view.epoxy.views.shimmer.AppListViewShimmerModel_
import com.aurora.store.viewmodel.category.CategoryViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CategoryFragment : BaseFragment<FragmentGenericRecyclerBinding>() {
    private val viewModel: CategoryViewModel by viewModels()

    companion object {
        @JvmStatic
        fun newInstance(pageType: Int): CategoryFragment {
            return CategoryFragment().apply {
                arguments = Bundle().apply {
                    putInt(Constants.PAGE_TYPE, pageType)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var pageType = 0
        val bundle = arguments
        if (bundle != null) {
            pageType = bundle.getInt(Constants.PAGE_TYPE, 0)
        }

        when (pageType) {
            0 -> viewModel.getCategoryList(Category.Type.APPLICATION)
            1 -> viewModel.getCategoryList(Category.Type.GAME)
        }

        val categoryType = when (pageType) {
            1 -> Category.Type.GAME
            else -> Category.Type.APPLICATION
        }

        viewModel.liveData.observe(viewLifecycleOwner) {
            when (it) {
                is ViewState.Success<*> -> {
                    val stash = it.getDataAs<CategoryStash>()
                    updateController(stash[categoryType])
                }

                else -> {
                    updateController(emptyList())
                }
            }
        }
    }

    fun updateController(categories: List<Category>?) {
        binding.recycler.withModels {
            if (categories == null) {
                for (i in 1..10) {
                    add(
                        AppListViewShimmerModel_()
                            .id(i)
                    )
                }
            } else {
                setFilterDuplicates(true)
                categories.forEach {
                    add(
                        CategoryViewModel_()
                            .id(it.title)
                            .category(it)
                            .click { _ -> openCategoryBrowseFragment(it) }
                    )
                }
            }
        }
    }
}
