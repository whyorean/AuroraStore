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
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.helpers.contracts.TopChartsContract.Chart
import com.aurora.gplayapi.helpers.contracts.TopChartsContract.Type
import com.aurora.store.TopChartStash
import com.aurora.store.data.model.ViewState
import com.aurora.store.data.model.ViewState.Empty.getDataAs
import com.aurora.store.databinding.FragmentTopContainerBinding
import com.aurora.store.view.custom.recycler.EndlessRecyclerOnScrollListener
import com.aurora.store.view.epoxy.views.AppProgressViewModel_
import com.aurora.store.view.epoxy.views.app.AppListViewModel_
import com.aurora.store.view.epoxy.views.shimmer.AppListViewShimmerModel_
import com.aurora.store.viewmodel.topchart.TopChartViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TopChartFragment : BaseFragment<FragmentTopContainerBinding>() {

    private val viewModel: TopChartViewModel by activityViewModels()

    private var streamCluster: StreamCluster? = StreamCluster()

    companion object {
        @JvmStatic
        fun newInstance(chartType: Int, chartCategory: Int): TopChartFragment {
            return TopChartFragment().apply {
                arguments = Bundle().apply {
                    putInt(Constants.TOP_CHART_TYPE, chartType)
                    putInt(Constants.TOP_CHART_CATEGORY, chartCategory)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var type = 0
        var category = 0
        val bundle = arguments

        if (bundle != null) {
            type = bundle.getInt(Constants.TOP_CHART_TYPE, 0)
            category = bundle.getInt(Constants.TOP_CHART_CATEGORY, 0)
        }

        val chartType = when (type) {
            1 -> Type.GAME
            else -> Type.APPLICATION
        }

        val chartCategory = when (category) {
            1 -> Chart.TOP_GROSSING
            2 -> Chart.MOVERS_SHAKERS
            3 -> Chart.TOP_SELLING_PAID
            else -> Chart.TOP_SELLING_FREE
        }

        binding.recycler.addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
            override fun onLoadMore(currentPage: Int) {
                viewModel.nextCluster(chartType, chartCategory)
            }
        })

        updateController(null)

        viewModel.getStreamCluster(chartType, chartCategory)
        viewModel.liveData.observe(viewLifecycleOwner) {
            when (it) {
                is ViewState.Loading, is ViewState.Error -> {
                    updateController(null)
                }

                is ViewState.Success<*> -> {
                    val stash = it.getDataAs<TopChartStash>()
                    streamCluster = stash[chartType]?.get(chartCategory)

                    updateController(streamCluster)
                }

                else -> {}
            }
        }
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
                streamCluster.clusterAppList.forEach { app ->
                    add(
                        AppListViewModel_()
                            .id(app.id)
                            .app(app)
                            .click { _ -> openDetailsFragment(app.packageName) }
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
