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
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.StreamBundle
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.store.R
import com.aurora.store.view.epoxy.groups.CarouselModelGroup
import com.aurora.store.view.epoxy.groups.CarouselShimmerGroup
import com.aurora.store.view.epoxy.views.app.AppListViewModel_
import com.aurora.store.view.epoxy.views.app.NoAppViewModel_

open class GenericCarouselController(private val callbacks: Callbacks) :

    TypedEpoxyController<StreamBundle?>() {

    interface Callbacks {
        fun onHeaderClicked(streamCluster: StreamCluster)
        fun onClusterScrolled(streamCluster: StreamCluster)
        fun onAppClick(app: App)
        fun onAppLongClick(app: App)
    }

    open fun applyFilter(streamBundle: StreamCluster): Boolean {
        return streamBundle.clusterTitle.isNotBlank()  //Filter noisy cluster
                && streamBundle.clusterAppList.isNotEmpty() //Filter empty clusters
                && streamBundle.clusterAppList.count() > 1 //Filter clusters with single apps (mostly promotions)
    }

    override fun buildModels(streamBundle: StreamBundle?) {
        setFilterDuplicates(true)
        if (streamBundle == null) {
            for (i in 1..4) {
                add(
                    CarouselShimmerGroup()
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
                if (streamBundle.streamClusters.size == 1) {
                    streamBundle
                        .streamClusters
                        .values
                        .filter { applyFilter(it) }
                        .forEach { streamCluster ->
                            streamCluster.clusterAppList.forEach {
                                add(
                                    AppListViewModel_()
                                        .id(it.id)
                                        .app(it)
                                        .click { _ -> callbacks.onAppClick(it) }
                                )
                            }
                        }

                } else {
                    streamBundle
                        .streamClusters
                        .values
                        .filter { applyFilter(it) }
                        .forEach { streamCluster ->
                            add(CarouselModelGroup(streamCluster, callbacks))
                        }

                }
                if (streamBundle.hasNext())
                    add(
                        CarouselShimmerGroup()
                            .id("progress")
                    )
            }
        }
    }
}
