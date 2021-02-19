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

package com.aurora.store.view.ui.apps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.aurora.Constants
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.store.R
import com.aurora.store.databinding.FragmentTopContainerBinding
import com.aurora.store.view.custom.recycler.EndlessRecyclerOnScrollListener
import com.aurora.store.view.epoxy.views.app.AppListViewModel_
import com.aurora.store.view.epoxy.views.AppProgressViewModel_
import com.aurora.store.view.epoxy.views.shimmer.AppListViewShimmerModel_
import com.aurora.store.view.ui.commons.BaseFragment
import com.aurora.store.viewmodel.topchart.*

class TopChartFragment : BaseFragment() {

    private lateinit var VM: BaseChartViewModel
    private lateinit var B: FragmentTopContainerBinding

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        B = FragmentTopContainerBinding.bind(
            inflater.inflate(
                R.layout.fragment_top_container,
                container,
                false
            )
        )

        val bundle = arguments
        if (bundle != null) {
            chartType = bundle.getInt(Constants.TOP_CHART_TYPE, 0)
            chartCategory = bundle.getInt(Constants.TOP_CHART_CATEGORY, 0)
        }

        when (chartCategory) {
            0 -> VM = ViewModelProvider(this).get(TopFreeAppChartViewModel::class.java)
            1 -> VM = ViewModelProvider(this).get(TopGrossingAppChartViewModel::class.java)
            2 -> VM = ViewModelProvider(this).get(TrendingAppChartViewModel::class.java)
            3 -> VM = ViewModelProvider(this).get(TopPaidAppChartViewModel::class.java)
        }

        return B.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        VM.liveData.observe(viewLifecycleOwner, {
            updateController(it)
        })

        B.recycler.addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
            override fun onLoadMore(currentPage: Int) {
                VM.nextCluster()
            }
        })

        updateController(null)
    }

    private fun updateController(streamCluster: StreamCluster?) {
        B.recycler.withModels {
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
                            .click { _ -> openDetailsActivity(app) }
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