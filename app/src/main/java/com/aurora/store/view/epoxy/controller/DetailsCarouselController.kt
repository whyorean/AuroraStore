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

import com.aurora.gplayapi.data.models.StreamBundle
import com.aurora.store.view.epoxy.groups.CarouselModelGroup
import com.aurora.store.view.epoxy.groups.CarouselShimmerGroup

class DetailsCarouselController(private val callbacks: Callbacks) :
    GenericCarouselController(callbacks) {

    override fun buildModels(streamBundle: StreamBundle?) {
        setFilterDuplicates(true)
        if (streamBundle == null) {
            for (i in 1..2) {
                add(
                    CarouselShimmerGroup()
                        .id(i)
                )
            }
        } else {
            streamBundle.streamClusters.values.filter { applyFilter(it) }
                .forEach { streamCluster ->
                    add(CarouselModelGroup(streamCluster, callbacks))
                }
        }
    }
}