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
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.StreamBundle
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.helpers.contracts.StreamContract
import com.aurora.store.R
import com.aurora.store.data.model.ViewState
import com.aurora.store.data.model.ViewState.Loading.getDataAs
import com.aurora.store.databinding.ActivityGenericRecyclerBinding
import com.aurora.store.view.custom.recycler.EndlessRecyclerOnScrollListener
import com.aurora.store.view.epoxy.controller.CategoryCarouselController
import com.aurora.store.view.epoxy.controller.GenericCarouselController
import com.aurora.store.viewmodel.subcategory.SubCategoryClusterViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CategoryBrowseFragment : BaseFragment(R.layout.activity_generic_recycler),
    GenericCarouselController.Callbacks {

    private var _binding: ActivityGenericRecyclerBinding? = null
    private val binding: ActivityGenericRecyclerBinding
        get() = _binding!!

    private val args: CategoryBrowseFragmentArgs by navArgs()
    private val viewModel: SubCategoryClusterViewModel by activityViewModels()

    private lateinit var category: StreamContract.Category
    private var streamBundle: StreamBundle? = StreamBundle()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = ActivityGenericRecyclerBinding.bind(view)

        val rawCategory = args.browseUrl.split("/").last()
        category = StreamContract.Category.NONE.apply { value = rawCategory }

        val genericCarouselController = CategoryCarouselController(this)

        // Toolbar
        binding.layoutToolbarAction.apply {
            txtTitle.text = args.title
            imgActionPrimary.setOnClickListener { findNavController().navigateUp() }
        }

        // RecyclerView
        binding.recycler.setController(genericCarouselController)
        binding.recycler.addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
            override fun onLoadMore(currentPage: Int) {
                viewModel.observe(category)
            }
        })

        viewModel.liveData.observe(viewLifecycleOwner) {
            when (it) {
                is ViewState.Loading -> {
                    genericCarouselController.setData(null)
                }

                is ViewState.Success<*> -> {
                    val stash = it.getDataAs<Map<String, StreamBundle>>()
                    streamBundle = stash[category.value]

                    genericCarouselController.setData(streamBundle)
                }

                else -> {}
            }
        }

        viewModel.observe(category)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onHeaderClicked(streamCluster: StreamCluster) {

    }

    override fun onClusterScrolled(streamCluster: StreamCluster) {
        viewModel.observeCluster(category, streamCluster)
    }

    override fun onAppClick(app: App) {
        openDetailsFragment(app.packageName, app)
    }

    override fun onAppLongClick(app: App) {
        findNavController().navigate(
            CategoryBrowseFragmentDirections.actionCategoryBrowseFragmentToAppPeekDialogSheet(app)
        )
    }
}
