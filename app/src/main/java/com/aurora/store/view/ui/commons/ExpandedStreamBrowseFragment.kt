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
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.store.databinding.FragmentGenericWithToolbarBinding
import com.aurora.store.view.custom.recycler.EndlessRecyclerOnScrollListener
import com.aurora.store.view.epoxy.views.AppProgressViewModel_
import com.aurora.store.view.epoxy.views.app.AppListViewModel_
import com.aurora.store.view.epoxy.views.shimmer.AppListViewShimmerModel_
import com.aurora.store.viewmodel.browse.ExpandedStreamBrowseViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ExpandedStreamBrowseFragment : BaseFragment<FragmentGenericWithToolbarBinding>() {
    private val args: ExpandedStreamBrowseFragmentArgs by navArgs()
    private val viewModel: ExpandedStreamBrowseViewModel by viewModels()

    private lateinit var endlessRecyclerOnScrollListener: EndlessRecyclerOnScrollListener
    private lateinit var cluster: StreamCluster

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Toolbar
        binding.toolbar.apply {
            title = args.title
            setNavigationOnClickListener { findNavController().navigateUp() }
        }

        viewModel.liveData.observe(viewLifecycleOwner) {
            if (!::cluster.isInitialized) attachRecycler()
            cluster = it

            updateController(cluster)
            updateTitle(cluster)
        }

        viewModel.getInitialCluster(args.expandedStreamUrl)
        updateController(null)
    }

    private fun updateTitle(streamCluster: StreamCluster) {
        if (streamCluster.clusterTitle.isNotEmpty())
            binding.toolbar.title = streamCluster.clusterTitle
    }

    private fun attachRecycler() {
        endlessRecyclerOnScrollListener = object : EndlessRecyclerOnScrollListener() {
            override fun onLoadMore(currentPage: Int) {
                viewModel.next()
            }
        }
        binding.recycler.addOnScrollListener(endlessRecyclerOnScrollListener)
    }

    private fun updateController(streamCluster: StreamCluster?) {
        binding.recycler.withModels {
            setFilterDuplicates(true)
            if (streamCluster == null) {
                for (i in 1..6) {
                    add(
                        AppListViewShimmerModel_()
                            .id(i)
                    )
                }
            } else {
                streamCluster.clusterAppList.forEach {
                    add(
                        AppListViewModel_()
                            .id(it.packageName.hashCode())
                            .app(it)
                            .click { _ -> openDetailsFragment(it.packageName, it) }
                    )
                }

                if (streamCluster.hasNext()) {
                    add(
                        AppProgressViewModel_()
                            .id("progress")
                    )
                }
            }
        }
    }
}
