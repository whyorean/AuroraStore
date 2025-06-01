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

package com.aurora.store.view.epoxy.controller

import com.airbnb.epoxy.TypedEpoxyController
import com.aurora.gplayapi.data.models.StreamBundle
import com.aurora.store.R
import com.aurora.store.view.epoxy.controller.GenericCarouselController.Callbacks
import com.aurora.store.view.epoxy.groups.CarouselModelGroup
import com.aurora.store.view.epoxy.views.app.AppListViewModel_
import com.aurora.store.view.epoxy.views.app.NoAppViewModel_
import com.aurora.store.view.epoxy.views.shimmer.AppListViewShimmerModel_

open class SearchCarouselController(private val callbacks: Callbacks) :

    TypedEpoxyController<StreamBundle?>() {

    override fun buildModels(streamBundle: StreamBundle?) {
        setFilterDuplicates(true)
        if (streamBundle == null) {
            for (i in 1..6) {
                add(
                    AppListViewShimmerModel_()
                        .id(i)
                )
            }
        } else {
            if (streamBundle.streamClusters.isEmpty()) {
                add(
                    NoAppViewModel_()
                        .id("no_app")
                        .icon(R.drawable.ic_apps)
                        .message(R.string.no_apps_available)
                )
            } else {
                streamBundle.streamClusters.values
                    .filter { it.clusterAppList.isNotEmpty() } // Filter out empty clusters, mostly related keywords
                    .forEach {
                        if (it.clusterTitle.isEmpty() or (it.clusterTitle == streamBundle.streamTitle)) {
                            if (it.clusterAppList.isNotEmpty()) {
                                it.clusterAppList.forEach { app ->
                                    add(
                                        AppListViewModel_()
                                            .id(app.id)
                                            .app(app)
                                            .click { _ -> callbacks.onAppClick(app) }
                                    )
                                }
                            }
                        } else {
                            add(CarouselModelGroup(it, callbacks))
                        }
                    }

                if (streamBundle.hasNext())
                    add(
                        AppListViewShimmerModel_()
                            .id("progress")
                    )
            }
        }
    }
}
