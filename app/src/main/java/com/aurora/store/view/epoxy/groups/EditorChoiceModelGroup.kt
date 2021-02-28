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
import com.aurora.gplayapi.data.models.editor.EditorChoiceCluster
import com.aurora.store.R
import com.aurora.store.view.epoxy.controller.EditorChoiceController
import com.aurora.store.view.epoxy.views.EditorHeadViewModel_
import com.aurora.store.view.epoxy.views.EditorImageViewModel_

class EditorChoiceModelGroup(
    editorChoiceCluster: EditorChoiceCluster,
    callbacks: EditorChoiceController.Callbacks
) : EpoxyModelGroup(
    R.layout.model_editorchoice_group,
    buildModels(editorChoiceCluster, callbacks)
) {
    companion object {
        private fun buildModels(
            editorChoiceCluster: EditorChoiceCluster,
            callbacks: EditorChoiceController.Callbacks
        ): List<EpoxyModel<*>> {

            val models = ArrayList<EpoxyModel<*>>()
            val clusterViewModels = mutableListOf<EpoxyModel<*>>()

            val idPrefix = editorChoiceCluster.id


            models.add(
                EditorImageViewModel_()
                    .id("artwork_header_${idPrefix}")
                    .artwork(editorChoiceCluster.clusterArtwork[0])
                    .click { _ -> callbacks.onClick(editorChoiceCluster) }

            )

            models.add(
                EditorHeadViewModel_()
                    .id("header_${idPrefix}")
                    .title(editorChoiceCluster.clusterTitle)
                    .click { _ -> callbacks.onClick(editorChoiceCluster) }
            )

            editorChoiceCluster.clusterArtwork
                .drop(1)
                .forEach {
                    clusterViewModels.add(
                        EditorImageViewModel_()
                            .id("artwork_${idPrefix}_${it.url}")
                            .artwork(it)
                            .click { _ -> callbacks.onClick(editorChoiceCluster) }

                    )
                }

            models.add(
                CarouselHorizontalModel_()
                    .id("cluster_${idPrefix}")
                    .models(clusterViewModels)
            )

            return models
        }
    }
}