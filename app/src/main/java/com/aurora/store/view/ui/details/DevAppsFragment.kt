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

package com.aurora.store.view.ui.details

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.store.AppStreamStash
import com.aurora.store.data.model.ViewState
import com.aurora.store.data.model.ViewState.Loading.getDataAs
import com.aurora.store.databinding.FragmentGenericWithToolbarBinding
import com.aurora.store.view.custom.recycler.EndlessRecyclerOnScrollListener
import com.aurora.store.view.epoxy.controller.GenericCarouselController
import com.aurora.store.view.epoxy.controller.SearchCarouselController
import com.aurora.store.view.ui.commons.BaseFragment
import com.aurora.store.viewmodel.search.SearchResultViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DevAppsFragment : BaseFragment<FragmentGenericWithToolbarBinding>(),
    GenericCarouselController.Callbacks {

    private val args: DevAppsFragmentArgs by navArgs()

    private val viewModel: SearchResultViewModel by viewModels()
    private val controller = SearchCarouselController(this)

    private var query: String = ""
        get() = "pub:${args.developerName}"

    private var scrollListener: EndlessRecyclerOnScrollListener =
        object : EndlessRecyclerOnScrollListener(visibleThreshold = 4) {
            override fun onLoadMore(currentPage: Int) {
                viewModel.observe(query)
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            toolbar.apply {
                title = args.developerName
                setNavigationOnClickListener { findNavController().navigateUp() }
            }

            recycler.setController(controller)
            recycler.addOnScrollListener(scrollListener)
        }

        with(viewModel) {
            search("pub:${args.developerName}")

            liveData.observe(viewLifecycleOwner) {
                when (it) {
                    is ViewState.Loading -> {
                        controller.setData(null)
                    }

                    is ViewState.Success<*> -> {
                        val stash = it.getDataAs<AppStreamStash>()
                        controller.setData(stash[query])
                    }

                    else -> {}
                }
            }
        }
    }

    override fun onHeaderClicked(streamCluster: StreamCluster) {
        openStreamBrowseFragment(streamCluster)
    }

    override fun onClusterScrolled(streamCluster: StreamCluster) {
        viewModel.observeCluster(query, streamCluster)
    }

    override fun onAppClick(app: App) {
        openDetailsFragment(app.packageName, app)
    }

    override fun onAppLongClick(app: App) {

    }
}
