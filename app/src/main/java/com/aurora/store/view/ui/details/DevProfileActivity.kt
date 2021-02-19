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
import com.airbnb.epoxy.EpoxyModel
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.data.models.details.DevStream
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.store.R
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.databinding.ActivityDevProfileBinding
import com.aurora.store.util.extensions.close
import com.aurora.store.util.extensions.load
import com.aurora.store.util.extensions.toast
import com.aurora.store.view.epoxy.groups.CarouselHorizontalModel_
import com.aurora.store.view.epoxy.views.AppListViewModel_
import com.aurora.store.view.epoxy.views.AppViewModel_
import com.aurora.store.view.epoxy.views.HeaderViewModel_
import com.aurora.store.view.epoxy.views.details.ScreenshotViewModel_
import com.aurora.store.view.ui.commons.BaseActivity
import nl.komponents.kovenant.task
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi

class DevProfileActivity : BaseActivity() {

    private lateinit var B: ActivityDevProfileBinding
    private lateinit var authData: AuthData

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
        B = ActivityDevProfileBinding.inflate(layoutInflater)
        authData = AuthProvider.with(this).getAuthData()

        setContentView(B.root)

        attachToolbar()

        B.viewFlipper.displayedChild = 1

        onNewIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) {
            if (intent.scheme != null && (intent.scheme == "http" || intent.scheme == "https")) {
                val devId = intent.data!!.getQueryParameter("id")
                if (devId.isNullOrEmpty()) {
                    close()
                } else {
                    fetchDevProfile(devId)
                }
            }
        } else {
            close()
        }
    }

    private fun attachToolbar() {
        B.layoutToolbarAction.toolbar.setOnClickListener {
            close()
        }
        B.layoutToolbarAction.txtTitle.text = getString(R.string.details_dev_profile)
    }

    private fun updateInfo(devStream: DevStream) {
        B.layoutToolbarAction.txtTitle.text = devStream.title
        B.txtDevName.text = devStream.title
        B.txtDevDescription.text = devStream.description
        B.imgIcon.load(devStream.imgUrl)
    }

    private fun updateController(devStream: DevStream) {
        B.recycler
            .withModels {
                setFilterDuplicates(true)

                for (entry in devStream.appListMap) {
                    val clusterViewModels = mutableListOf<EpoxyModel<*>>()
                    val screenshotsViewModels = mutableListOf<EpoxyModel<*>>()

                    if (entry.value.size == 1) {
                        val app = entry.value[0]
                        for (artwork in app.screenshots) {
                            screenshotsViewModels.add(
                                ScreenshotViewModel_()
                                    .id(artwork.url)
                                    .artwork(artwork)
                            )
                        }

                        clusterViewModels.add(
                            AppListViewModel_()
                                .id(app.id)
                                .app(app)
                                .click { _ ->
                                    openDetailsActivity(app)
                                }
                        )
                    } else {
                        for (app in entry.value) {
                            clusterViewModels.add(
                                AppViewModel_()
                                    .id(app.id)
                                    .app(app)
                                    .click { _ ->
                                        openDetailsActivity(app)
                                    }
                            )
                        }
                    }

                    add(
                        HeaderViewModel_()
                            .id(entry.key)
                            .title(entry.key)
                    )

                    if (screenshotsViewModels.isNotEmpty()) {
                        add(
                            CarouselHorizontalModel_()
                                .id("${entry.key}_screenshots")
                                .models(screenshotsViewModels)
                        )
                    }

                    add(
                        CarouselHorizontalModel_()
                            .id("${entry.key}_cluster")
                            .models(clusterViewModels)
                    )
                }
            }
    }

    private fun fetchDevProfile(devId: String) {
        task {
            AppDetailsHelper(authData).getDeveloperStream(devId)
        } successUi {
            B.viewFlipper.displayedChild = 0
            updateInfo(it)
            updateController(it)
        } failUi {
            toast("Dev profile unavailable")
            close()
        }
    }
}