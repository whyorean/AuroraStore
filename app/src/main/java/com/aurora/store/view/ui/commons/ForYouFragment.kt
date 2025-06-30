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
import com.aurora.Constants
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.StreamBundle
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.helpers.contracts.StreamContract.Category
import com.aurora.gplayapi.helpers.contracts.StreamContract.Type
import com.aurora.store.HomeStash
import com.aurora.store.data.model.ViewState
import com.aurora.store.data.model.ViewState.Loading.getDataAs
import com.aurora.store.databinding.FragmentForYouBinding
import com.aurora.store.view.custom.recycler.EndlessRecyclerOnScrollListener
import com.aurora.store.view.epoxy.controller.GenericCarouselController
import com.aurora.store.viewmodel.homestream.StreamViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForYouFragment : BaseFragment<FragmentForYouBinding>(),
    GenericCarouselController.Callbacks {
    private val viewModel: StreamViewModel by activityViewModels()

    private var category: Category = Category.APPLICATION
    private var streamBundle: StreamBundle? = StreamBundle()

    companion object {
        @JvmStatic
        fun newInstance(pageType: Int): ForYouFragment {
            return ForYouFragment().apply {
                arguments = Bundle().apply {
                    putInt(Constants.PAGE_TYPE, pageType)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val genericCarouselController = GenericCarouselController(this)

        var pageType = 0
        val bundle = arguments
        if (bundle != null) {
            pageType = bundle.getInt(Constants.PAGE_TYPE, 0)
        }

        category = if (pageType == 0) Category.APPLICATION else Category.GAME

        binding.recycler.setController(genericCarouselController)
        binding.recycler.addOnScrollListener(
            object : EndlessRecyclerOnScrollListener(visibleThreshold = 4) {
                override fun onLoadMore(currentPage: Int) {
                    viewModel.observe(category, Type.HOME)
                }
            }
        )

        viewModel.getStreamBundle(category, Type.HOME)
        viewModel.liveData.observe(viewLifecycleOwner) {
            when (it) {
                is ViewState.Loading -> {
                    genericCarouselController.setData(null)
                }

                is ViewState.Success<*> -> {
                    val stash = it.getDataAs<HomeStash>()
                    streamBundle = stash[category]

                    genericCarouselController.setData(streamBundle)
                }

                else -> {}
            }
        }
    }

    override fun onHeaderClicked(streamCluster: StreamCluster) {
        openStreamBrowseFragment(streamCluster)
    }

    override fun onClusterScrolled(streamCluster: StreamCluster) {
        viewModel.observeCluster(category, streamCluster)
    }

    override fun onAppClick(app: App) {
        openDetailsFragment(app.packageName)
    }

    override fun onAppLongClick(app: App) {

    }
}
