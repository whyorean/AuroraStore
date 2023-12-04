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
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.helpers.TopChartsHelper.Chart
import com.aurora.gplayapi.helpers.TopChartsHelper.Type
import com.aurora.store.R
import com.aurora.store.databinding.FragmentTopContainerBinding
import com.aurora.store.view.custom.recycler.EndlessRecyclerOnScrollListener
import com.aurora.store.view.epoxy.views.AppProgressViewModel_
import com.aurora.store.view.epoxy.views.app.AppListViewModel_
import com.aurora.store.view.epoxy.views.shimmer.AppListViewShimmerModel_
import com.aurora.store.viewmodel.topchart.TopChartViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TopChartFragment : BaseFragment(R.layout.fragment_top_container) {

    private var _binding: FragmentTopContainerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TopChartViewModel by viewModels()

    private var chartType = 0
    private var chartCategory = 0

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
        _binding = FragmentTopContainerBinding.bind(view)

        val bundle = arguments
        if (bundle != null) {
            chartType = bundle.getInt(Constants.TOP_CHART_TYPE, 0)
            chartCategory = bundle.getInt(Constants.TOP_CHART_CATEGORY, 0)
        }

        when (chartType) {
            0 -> when (chartCategory) {
                0 -> viewModel.getStreamCluster(Type.APPLICATION, Chart.TOP_SELLING_FREE)
                1 -> viewModel.getStreamCluster(Type.APPLICATION, Chart.TOP_GROSSING)
                2 -> viewModel.getStreamCluster(Type.APPLICATION, Chart.MOVERS_SHAKERS)
                3 -> viewModel.getStreamCluster(Type.APPLICATION, Chart.TOP_SELLING_PAID)
            }

            1 -> when (chartCategory) {
                0 -> viewModel.getStreamCluster(Type.GAME, Chart.TOP_SELLING_FREE)
                1 -> viewModel.getStreamCluster(Type.GAME, Chart.TOP_GROSSING)
                2 -> viewModel.getStreamCluster(Type.GAME, Chart.MOVERS_SHAKERS)
                3 -> viewModel.getStreamCluster(Type.GAME, Chart.TOP_SELLING_PAID)
            }
        }

        viewModel.liveData.observe(viewLifecycleOwner) {
            updateController(it)
        }

        binding.recycler.addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
            override fun onLoadMore(currentPage: Int) {
                viewModel.nextCluster()
            }
        })

        updateController(null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
                            .click { _ -> openDetailsFragment(app.packageName, app) }
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
