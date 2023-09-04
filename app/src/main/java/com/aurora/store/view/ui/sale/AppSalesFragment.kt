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
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.databinding.ActivityGenericRecyclerBinding
import com.aurora.store.view.custom.recycler.EndlessRecyclerOnScrollListener
import com.aurora.store.view.epoxy.views.AppProgressViewModel_
import com.aurora.store.view.epoxy.views.app.AppListViewModel_
import com.aurora.store.view.epoxy.views.shimmer.AppListViewShimmerModel_
import com.aurora.store.view.ui.commons.BaseFragment
import com.aurora.store.viewmodel.sale.AppSalesViewModel


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
                VM.next()
            }
        }
        binding.recycler.addOnScrollListener(endlessRecyclerOnScrollListener)
        updateController(null)

        VM.liveAppList.observe(viewLifecycleOwner) {
            updateController(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateController(appList: List<App>?) {
        binding.recycler
            .withModels {
                setFilterDuplicates(true)
                if (appList == null) {
                    for (i in 1..6) {
                        add(
                            AppListViewShimmerModel_()
                                .id(i)
                        )
                    }
                } else {
                    appList
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

                    if (appList.isNotEmpty()) {
                        add(
                            AppProgressViewModel_()
                                .id("progress")
                        )
                    }
                }
            }
    }
}
