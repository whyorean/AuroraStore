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

package com.aurora.store.view.epoxy.views

import android.view.View
import android.view.animation.AnimationUtils
import com.airbnb.epoxy.EpoxyModel
import com.aurora.store.view.epoxy.views.app.AppListView

abstract class BaseModel<T : View> : EpoxyModel<T>() {

    override fun bind(view: T) {
        super.bind(view)
        when (view) {
            is AppListView -> {
                view.startAnimation(
                    AnimationUtils.loadAnimation(
                        view.context,
                        android.R.anim.fade_in
                    )
                )
            }
        }
    }

    override fun unbind(view: T) {
        when (view) {
            is AppListView -> {
                view.clearAnimation()
            }
        }
    }
}
