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
import androidx.lifecycle.ViewModelProvider
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.databinding.ActivityGenericRecyclerBinding
import com.aurora.store.util.extensions.close
import com.aurora.store.view.custom.recycler.EndlessRecyclerOnScrollListener
import com.aurora.store.view.epoxy.views.app.AppListViewModel_
import com.aurora.store.view.epoxy.views.AppProgressViewModel_
import com.aurora.store.view.epoxy.views.shimmer.AppListViewShimmerModel_
import com.aurora.store.view.ui.commons.BaseActivity
import com.aurora.store.viewmodel.sale.AppSalesViewModel


class AppSalesActivity : BaseActivity() {

    lateinit var B: ActivityGenericRecyclerBinding
    lateinit var VM: AppSalesViewModel

    lateinit var endlessRecyclerOnScrollListener: EndlessRecyclerOnScrollListener

    override fun onConnected() {
        hideNetworkConnectivitySheet()
    }

    override fun onDisconnected() {
        showNetworkConnectivitySheet()
    }

    override fun onReconnected() {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        B = ActivityGenericRecyclerBinding.inflate(layoutInflater)
        VM = ViewModelProvider(this).get(AppSalesViewModel::class.java)

        setContentView(B.root)

        attachRecycler()
        attachToolbar()

        VM.liveAppList.observe(this, {
            updateController(it)
        })
    }

    private fun attachToolbar() {
        B.layoutToolbarAction.txtTitle.text = getString(R.string.title_apps_sale)
        B.layoutToolbarAction.imgActionPrimary.setOnClickListener {
            close()
        }
    }

    private fun attachRecycler() {
        endlessRecyclerOnScrollListener = object : EndlessRecyclerOnScrollListener() {
            override fun onLoadMore(currentPage: Int) {
                VM.next()
            }
        }
        B.recycler.addOnScrollListener(endlessRecyclerOnScrollListener)
        updateController(null)
    }

    private fun updateController(appList: List<App>?) {
        B.recycler
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
                                    .click { _ -> openDetailsActivity(it) }
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