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

package com.aurora.store.view.ui.all

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.aurora.store.R
import com.aurora.store.databinding.FragmentAppsBinding
import com.aurora.store.view.custom.recycler.EndlessRecyclerOnScrollListener
import com.aurora.store.view.epoxy.views.AppProgressViewModel_
import com.aurora.store.view.epoxy.views.app.AppListViewModel_
import com.aurora.store.view.epoxy.views.shimmer.AppListViewShimmerModel_
import com.aurora.store.view.ui.commons.BaseFragment
import com.aurora.store.viewmodel.all.PaginatedAppList
import com.aurora.store.viewmodel.all.PurchasedViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PurchasedAppsFragment : BaseFragment<FragmentAppsBinding>() {
    private val viewModel: PurchasedViewModel by viewModels()

    companion object {
        @JvmStatic
        fun newInstance(): PurchasedAppsFragment {
            return PurchasedAppsFragment()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val endlessRecyclerOnScrollListener = object : EndlessRecyclerOnScrollListener(8) {
            override fun onLoadMore(currentPage: Int) {
                viewModel.observe()
            }
        }

        binding.recycler.addOnScrollListener(endlessRecyclerOnScrollListener)

        viewModel.liveData.observe(viewLifecycleOwner) {
            updateController(it)
        }

        updateController(null)
    }

    private fun updateController(paginatedAppList: PaginatedAppList?) {
        binding.recycler.withModels {
            setFilterDuplicates(true)
            if (paginatedAppList == null) {
                for (i in 1..10) {
                    add(
                        AppListViewShimmerModel_()
                            .id(i)
                    )
                }
            } else {
                paginatedAppList.appList.forEach { app ->
                    add(
                        AppListViewModel_()
                            .id(app.id)
                            .app(app)
                            .click { _ -> openDetailsFragment(app.packageName, app) }
                            .longClick { _ ->
                                openAppMenuSheet(app)
                                false
                            }
                    )
                }

                if (paginatedAppList.hasMore) {
                    add(
                        AppProgressViewModel_()
                            .id("progress")
                    )
                }
            }
        }
    }
}
