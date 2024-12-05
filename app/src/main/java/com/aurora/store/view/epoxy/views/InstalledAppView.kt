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

import android.content.Context
import android.util.AttributeSet
import coil3.load
import coil3.request.placeholder
import coil3.request.transformations
import coil3.transform.RoundedCornersTransformation
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.aurora.store.R
import com.aurora.store.data.model.MinimalApp
import com.aurora.store.databinding.ViewPackageBinding

@ModelView(
    autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT,
    baseModelClass = BaseModel::class
)
class InstalledAppView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseView<ViewPackageBinding>(context, attrs, defStyleAttr) {

    @ModelProp(options = [ModelProp.Option.IgnoreRequireHashCode])
    fun packageInfo(app: MinimalApp) {
        binding.imgIcon.load(app.icon) {
            placeholder(R.drawable.bg_placeholder)
            transformations(RoundedCornersTransformation(25F))
        }

        binding.txtLine1.text = app.displayName
        binding.txtLine2.text = app.packageName
        binding.txtLine3.text = ("${app.versionName} (${app.versionCode})")
    }

    @CallbackProp
    fun click(onClickListener: OnClickListener?) {
        binding.root.setOnClickListener(onClickListener)
    }

    @CallbackProp
    fun longClick(onClickListener: OnLongClickListener?) {
        binding.root.setOnLongClickListener(onClickListener)
    }
}
