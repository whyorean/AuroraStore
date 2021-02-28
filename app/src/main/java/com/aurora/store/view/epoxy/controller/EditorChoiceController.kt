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
import com.aurora.gplayapi.data.models.editor.EditorChoiceBundle
import com.aurora.gplayapi.data.models.editor.EditorChoiceCluster
import com.aurora.store.view.epoxy.groups.EditorChoiceModelGroup
import com.aurora.store.view.epoxy.views.HeaderViewModel_

class EditorChoiceController(private val callbacks: Callbacks) :
    TypedEpoxyController<List<EditorChoiceBundle>>() {

    interface Callbacks {
        fun onClick(editorChoiceCluster: EditorChoiceCluster)
    }

    override fun buildModels(editorChoiceBundles: List<EditorChoiceBundle>) {
        editorChoiceBundles.forEach { editorChoiceBundle ->
            val idPrefix = editorChoiceBundle.id

            add(
                HeaderViewModel_()
                    .id("header_${idPrefix}")
                    .title(editorChoiceBundle.bundleTitle)
            )

            editorChoiceBundle.bundleChoiceClusters.forEach {
                add(EditorChoiceModelGroup(it, callbacks))
            }
        }
    }
}