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

package com.aurora.store.view.ui.details

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.aurora.Constants
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.SearchBundle
import com.aurora.store.databinding.ActivityGenericRecyclerBinding
import com.aurora.extensions.close
import com.aurora.store.view.custom.recycler.EndlessRecyclerOnScrollListener
import com.aurora.store.view.epoxy.views.app.AppListViewModel_
import com.aurora.store.view.epoxy.views.AppProgressViewModel_
import com.aurora.store.view.ui.commons.BaseActivity
import com.aurora.store.viewmodel.search.SearchResultViewModel

class DevAppsActivity : BaseActivity() {

    private lateinit var B: ActivityGenericRecyclerBinding
    private lateinit var VM: SearchResultViewModel

    private lateinit var endlessRecyclerOnScrollListener: EndlessRecyclerOnScrollListener
    private lateinit var app: App

    var searchBundle: SearchBundle = SearchBundle()

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
        VM = ViewModelProvider(this)[SearchResultViewModel::class.java]

        setContentView(B.root)

        VM.liveData.observe(this, {
            searchBundle = it
            updateController(searchBundle)
        })

        attachRecycler()

        onNewIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            val rawApp: String? = intent.getStringExtra(Constants.STRING_APP)
            app = gson.fromJson(rawApp, App::class.java)
            app.let {
                attachToolbar()
                VM.observeSearchResults("pub:${app.developerName}")
            }
        }
    }

    private fun attachToolbar() {
        B.layoutToolbarAction.toolbar.setOnClickListener {
            close()
        }
        B.layoutToolbarAction.txtTitle.text = app.developerName
    }

    private fun attachRecycler() {
        endlessRecyclerOnScrollListener = object : EndlessRecyclerOnScrollListener() {
            override fun onLoadMore(currentPage: Int) {
                VM.next(searchBundle.subBundles)
            }
        }
        B.recycler.addOnScrollListener(endlessRecyclerOnScrollListener)
    }

    private fun updateController(searchBundle: SearchBundle) {
        B.recycler
            .withModels {
                setFilterDuplicates(true)
                searchBundle.appList.forEach { app ->
                    add(
                        AppListViewModel_()
                            .id(app.id)
                            .app(app)
                            .click(View.OnClickListener {
                                openDetailsActivity(app)
                            })
                    )
                }

                if (searchBundle.subBundles.isNotEmpty()) {
                    add(
                        AppProgressViewModel_()
                            .id("progress")
                    )
                }
            }
    }
}
