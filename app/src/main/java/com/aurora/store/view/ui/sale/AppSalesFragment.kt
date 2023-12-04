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

package com.aurora.store.view.ui.sale

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.aurora.store.R
import com.aurora.store.databinding.ActivityGenericRecyclerBinding
import com.aurora.store.view.custom.recycler.EndlessRecyclerOnScrollListener
import com.aurora.store.view.epoxy.views.AppProgressViewModel_
import com.aurora.store.view.epoxy.views.HeaderViewModel_
import com.aurora.store.view.epoxy.views.app.AppListViewModel_
import com.aurora.store.view.epoxy.views.app.NoAppViewModel_
import com.aurora.store.view.epoxy.views.shimmer.AppListViewShimmerModel_
import com.aurora.store.view.ui.commons.BaseFragment
import com.aurora.store.viewmodel.all.PaginatedAppList
import com.aurora.store.viewmodel.sale.AppSalesViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AppSalesFragment : BaseFragment(R.layout.activity_generic_recycler) {

    private var _binding: ActivityGenericRecyclerBinding? = null
    private val binding: ActivityGenericRecyclerBinding
        get() = _binding!!

    lateinit var VM: AppSalesViewModel

    lateinit var endlessRecyclerOnScrollListener: EndlessRecyclerOnScrollListener

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = ActivityGenericRecyclerBinding.bind(view)
        VM = ViewModelProvider(this)[AppSalesViewModel::class.java]

        binding.layoutToolbarAction.txtTitle.text = getString(R.string.title_apps_sale)
        binding.layoutToolbarAction.imgActionPrimary.setOnClickListener {
            findNavController().navigateUp()
        }

        endlessRecyclerOnScrollListener = object : EndlessRecyclerOnScrollListener() {
            override fun onLoadMore(currentPage: Int) {
                VM.observe()
            }
        }
        binding.recycler.addOnScrollListener(endlessRecyclerOnScrollListener)
        updateController(null)

        VM.liveData.observe(viewLifecycleOwner) {
            updateController(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateController(paginatedAppList: PaginatedAppList?) {
        binding.recycler
            .withModels {
                setFilterDuplicates(true)
                if (paginatedAppList == null) {
                    for (i in 1..6) {
                        add(
                            AppListViewShimmerModel_()
                                .id(i)
                        )
                    }
                } else {
                    add(
                        HeaderViewModel_()
                            .id("header")
                            .title(getString(R.string.title_apps_sale_provider))
                    )

                    paginatedAppList.appList
                        .filter { it.packageName.isNotEmpty() }
                        .forEach {
                            add(
                                AppListViewModel_()
                                    .id(it.packageName.hashCode())
                                    .app(it)
                                    .click { _ -> openDetailsFragment(it.packageName, it) }
                            )
                            setFilterDuplicates(true)
                        }

                    if (paginatedAppList.hasMore) {
                        add(
                            AppProgressViewModel_()
                                .id("progress")
                        )
                    }

                    if (!paginatedAppList.hasMore && paginatedAppList.appList.isEmpty()) {
                        add(
                            NoAppViewModel_()
                                .id("no_app_sale")
                                .icon(R.drawable.ic_apps)
                                .message(getString(R.string.details_no_apps_on_sale))
                        )
                    }
                }
            }
    }
}
