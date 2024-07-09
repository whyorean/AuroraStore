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
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.StreamBundle
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.helpers.StreamHelper.Category
import com.aurora.gplayapi.helpers.StreamHelper.Type
import com.aurora.store.R
import com.aurora.store.data.model.ViewState
import com.aurora.store.databinding.FragmentForYouBinding
import com.aurora.store.view.custom.recycler.EndlessRecyclerOnScrollListener
import com.aurora.store.view.epoxy.controller.GenericCarouselController
import com.aurora.store.viewmodel.homestream.BaseClusterViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForYouFragment : BaseFragment(R.layout.fragment_for_you),
    GenericCarouselController.Callbacks {

    private var _binding: FragmentForYouBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BaseClusterViewModel by viewModels()

    private lateinit var streamBundle: StreamBundle

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
        _binding = FragmentForYouBinding.bind(view)

        val genericCarouselController = GenericCarouselController(this)

        var pageType = 0
        val bundle = arguments
        if (bundle != null) {
            pageType = bundle.getInt(Constants.PAGE_TYPE, 0)
        }

        when (pageType) {
            0 -> viewModel.getStreamBundle(Category.APPLICATION, Type.HOME)
            1 -> viewModel.getStreamBundle(Category.GAME, Type.HOME)
        }

        binding.recycler.setController(genericCarouselController)

        viewModel.liveData.observe(viewLifecycleOwner) {
            when (it) {
                is ViewState.Empty -> {
                }

                is ViewState.Loading -> {
                    genericCarouselController.setData(null)
                }

                is ViewState.Error -> {

                }

                is ViewState.Status -> {

                }

                is ViewState.Success<*> -> {
                    if (!::streamBundle.isInitialized) {
                        binding.recycler.addOnScrollListener(
                            object : EndlessRecyclerOnScrollListener() {
                                override fun onLoadMore(currentPage: Int) {
                                    viewModel.observe()
                                }
                            }
                        )
                    }

                    streamBundle = it.data as StreamBundle
                    genericCarouselController.setData(streamBundle)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onHeaderClicked(streamCluster: StreamCluster) {
        if (streamCluster.clusterBrowseUrl.isNotEmpty())
            openStreamBrowseFragment(streamCluster.clusterBrowseUrl, streamCluster.clusterTitle)
    }

    override fun onClusterScrolled(streamCluster: StreamCluster) {
        viewModel.observeCluster(streamCluster)
    }

    override fun onAppClick(app: App) {
        openDetailsFragment(app.packageName, app)
    }

    override fun onAppLongClick(app: App) {

    }
}
