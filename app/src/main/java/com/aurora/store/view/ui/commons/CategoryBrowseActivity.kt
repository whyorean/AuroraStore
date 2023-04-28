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
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.aurora.Constants
import com.aurora.extensions.close
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.StreamBundle
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.store.R
import com.aurora.store.data.ViewState
import com.aurora.store.databinding.ActivityGenericRecyclerBinding
import com.aurora.store.view.custom.recycler.EndlessRecyclerOnScrollListener
import com.aurora.store.view.epoxy.controller.CategoryCarouselController
import com.aurora.store.view.epoxy.controller.GenericCarouselController
import com.aurora.store.view.ui.sheets.AppPeekDialogSheet
import com.aurora.store.viewmodel.subcategory.SubCategoryClusterViewModel


class CategoryBrowseActivity : BaseActivity(), GenericCarouselController.Callbacks {

    lateinit var B: ActivityGenericRecyclerBinding
    lateinit var C: GenericCarouselController
    lateinit var VM: SubCategoryClusterViewModel

    lateinit var endlessRecyclerOnScrollListener: EndlessRecyclerOnScrollListener

    lateinit var title: String
    lateinit var homeUrl: String

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
        C = CategoryCarouselController(this)
        VM = ViewModelProvider(this)[SubCategoryClusterViewModel::class.java]

        setContentView(B.root)

        attachToolbar()
        attachRecycler()

        intent.apply {
            homeUrl = getStringExtra(Constants.BROWSE_EXTRA).toString()
            title = getStringExtra(Constants.STRING_EXTRA).toString()
            VM.observeCategory(homeUrl)
            updateTitle(title)
        }

        updateController(null)
    }

    private fun updateTitle(title: String) {
        B.layoutToolbarAction.txtTitle.text = title
    }

    private fun attachToolbar() {
        B.layoutToolbarAction.imgActionPrimary.setOnClickListener {
            close()
        }
    }

    private fun attachRecycler() {

        B.recycler.setController(C)

        VM.liveData.observe(this, {
            when (it) {
                is ViewState.Empty -> {
                }
                is ViewState.Loading -> {
                    updateController(null)
                }
                is ViewState.Error -> {

                }
                is ViewState.Success<*> -> {
                    updateController(it.data as StreamBundle)
                }
                else -> {}
            }
        })

        endlessRecyclerOnScrollListener =
            object : EndlessRecyclerOnScrollListener() {
                override fun onLoadMore(currentPage: Int) {
                    VM.observe()
                }
            }

        endlessRecyclerOnScrollListener.disable()

        B.recycler.addOnScrollListener(endlessRecyclerOnScrollListener)
    }

    private fun updateController(streamBundle: StreamBundle?) {
        if (streamBundle != null)
            endlessRecyclerOnScrollListener.enable()
        C.setData(streamBundle)
    }

    override fun onHeaderClicked(streamCluster: StreamCluster) {
        if (streamCluster.clusterBrowseUrl.isNotEmpty())
            openStreamBrowseActivity(streamCluster.clusterBrowseUrl)
        else
            Toast.makeText(this, getString(R.string.toast_page_unavailable), Toast.LENGTH_SHORT)
                .show()
    }

    override fun onClusterScrolled(streamCluster: StreamCluster) {
        VM.observeCluster(streamCluster)
    }

    override fun onAppClick(app: App) {
        openDetailsActivity(app)
    }

    override fun onAppLongClick(app: App) {
        AppPeekDialogSheet.newInstance(app).show(supportFragmentManager, "APDS")
    }
}
