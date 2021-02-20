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
import androidx.lifecycle.ViewModelProvider
import com.aurora.Constants
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.store.databinding.ActivityGenericRecyclerBinding
import com.aurora.store.util.extensions.close
import com.aurora.store.view.custom.recycler.EndlessRecyclerOnScrollListener
import com.aurora.store.view.epoxy.views.AppProgressViewModel_
import com.aurora.store.view.epoxy.views.app.AppListViewModel_
import com.aurora.store.view.epoxy.views.shimmer.AppListViewShimmerModel_
import com.aurora.store.viewmodel.browse.ExpandedStreamBrowseViewModel


class ExpandedStreamBrowseActivity : BaseActivity() {

    lateinit var B: ActivityGenericRecyclerBinding
    lateinit var VM: ExpandedStreamBrowseViewModel

    lateinit var endlessRecyclerOnScrollListener: EndlessRecyclerOnScrollListener

    lateinit var title: String
    lateinit var cluster: StreamCluster

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
        VM = ViewModelProvider(this).get(ExpandedStreamBrowseViewModel::class.java)

        setContentView(B.root)

        attachToolbar()

        VM.liveData.observe(this, {
            if (!::cluster.isInitialized)
                attachRecycler()

            cluster = it

            updateController(cluster)
            updateTitle(cluster)
        })

        intent.apply {
            getStringExtra(Constants.BROWSE_EXTRA)?.let {
                VM.getInitialCluster(it)
            }
            getStringExtra(Constants.STRING_EXTRA)?.let {
                B.layoutToolbarAction.txtTitle.text = it
            }
        }

        updateController(null)
    }

    private fun updateTitle(streamCluster: StreamCluster) {
        if (streamCluster.clusterTitle.isNotEmpty())
            B.layoutToolbarAction.txtTitle.text = streamCluster.clusterTitle
    }

    private fun attachToolbar() {
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
                streamCluster.clusterAppList.forEach {

                    add(
                        AppListViewModel_()
                            .id(it.packageName.hashCode())
                            .app(it)
                            .click { _ -> openDetailsActivity(it) }
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