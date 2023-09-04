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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.databinding.FragmentUpdatesBinding
import com.aurora.store.view.custom.recycler.EndlessRecyclerOnScrollListener
import com.aurora.store.view.epoxy.views.HeaderViewModel_
import com.aurora.store.view.epoxy.views.app.AppListViewModel_
import com.aurora.store.view.epoxy.views.shimmer.AppListViewShimmerModel_
import com.aurora.store.view.ui.commons.BaseFragment
import com.aurora.store.viewmodel.all.PurchasedViewModel

class PurchasedAppsFragment : BaseFragment() {

    private lateinit var VM: PurchasedViewModel
    private lateinit var B: FragmentUpdatesBinding
    lateinit var endlessRecyclerOnScrollListener: EndlessRecyclerOnScrollListener

    companion object {
        @JvmStatic
        fun newInstance(): PurchasedAppsFragment {
            return PurchasedAppsFragment().apply {

            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        B = FragmentUpdatesBinding.bind(
            inflater.inflate(
                R.layout.fragment_updates,
                container,
                false
            )
        )

        VM = ViewModelProvider(requireActivity()).get(PurchasedViewModel::class.java)

        return B.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        VM.liveData.observe(viewLifecycleOwner) {
            updateController(it)
            B.swipeRefreshLayout.isRefreshing = false
        }

        B.swipeRefreshLayout.setOnRefreshListener {
            VM.observe()
        }

        attachRecycler()

        updateController(null)
    }

    private fun attachRecycler() {
        endlessRecyclerOnScrollListener = object : EndlessRecyclerOnScrollListener() {
            override fun onLoadMore(currentPage: Int) {
                VM.observe()
            }
        }
        B.recycler.addOnScrollListener(endlessRecyclerOnScrollListener)
    }


    private fun updateController(appList: List<App>?) {
        B.recycler.withModels {
            setFilterDuplicates(true)
            if (appList == null) {
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
                        .title("${appList.size} apps purchased")
                )
                appList.forEach { app ->
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
            }
        }
    }
}
