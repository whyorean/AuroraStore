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

package com.aurora.store.view.epoxy.groups

import com.airbnb.epoxy.EpoxyModel
import com.airbnb.epoxy.EpoxyModelGroup
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.store.R
import com.aurora.store.util.Log
import com.aurora.store.view.epoxy.controller.GenericCarouselController
import com.aurora.store.view.epoxy.views.HeaderViewModel_
import com.aurora.store.view.epoxy.views.app.AppViewModel_
import com.aurora.store.view.epoxy.views.shimmer.AppViewShimmerModel_

class CarouselModelGroup(
    streamCluster: StreamCluster,
    callbacks: GenericCarouselController.Callbacks
) :
    EpoxyModelGroup(
        R.layout.model_carousel_group, buildModels(
            streamCluster,
            callbacks
        )
    ) {
    companion object {
        private fun buildModels(
            streamCluster: StreamCluster,
            callbacks: GenericCarouselController.Callbacks
        ): List<EpoxyModel<*>> {
            val models = ArrayList<EpoxyModel<*>>()
            val clusterViewModels = mutableListOf<EpoxyModel<*>>()

            val idPrefix = streamCluster.clusterTitle.hashCode()

            models.add(
                HeaderViewModel_()
                    .id("${idPrefix}_header")
                    .title(streamCluster.clusterTitle)
                    .browseUrl(streamCluster.clusterBrowseUrl)
                    .click { _ ->
                        callbacks.onHeaderClicked(streamCluster)
                    }
            )

            for (app in streamCluster.clusterAppList) {
                clusterViewModels.add(
                    AppViewModel_()
                        .id(app.id)
                        .app(app)
                        .click { _ ->
                            callbacks.onAppClick(app)
                        }
                        .longClick { _ ->
                            callbacks.onAppLongClick(app)
                            false
                        }
                        .onBind { _, _, position ->
                            val itemCount = clusterViewModels.count()
                            if (itemCount >= 2) {
                                if (position == clusterViewModels.count() - 2) {
                                    callbacks.onClusterScrolled(streamCluster)
                                    Log.i("Cluster %s Scrolled", streamCluster.clusterTitle)
                                }
                            }
                        }
                )
            }

            if (streamCluster.hasNext()) {
                clusterViewModels.add(
                    AppViewShimmerModel_()
                        .id("${idPrefix}_progress")
                )
            }

            models.add(
                CarouselHorizontalModel_()
                    .id("${idPrefix}_cluster")
                    .models(clusterViewModels)
            )

            return models
        }
    }
}