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

package com.aurora.store.view.epoxy.views.details

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.airbnb.epoxy.OnViewRecycled
import com.aurora.store.R
import com.aurora.store.databinding.ViewInfoBinding
import com.aurora.store.view.epoxy.views.BaseModel
import com.aurora.store.view.epoxy.views.BaseView

@ModelView(
    autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT,
    baseModelClass = BaseModel::class
)
class InfoView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseView<ViewInfoBinding>(context, attrs, defStyleAttr) {

    @ModelProp(options = [ModelProp.Option.IgnoreRequireHashCode])
    fun badge(info: Map.Entry<String, String>) {
        binding.txtTitle.text = when (info.key) {
            "DOWNLOAD" -> ContextCompat.getString(context, R.string.app_info_downloads)
            "UPDATED_ON" -> ContextCompat.getString(context, R.string.app_info_updated_on)
            "REQUIRES" -> ContextCompat.getString(context, R.string.app_info_min_android)
            "TARGET" -> ContextCompat.getString(context, R.string.app_info_target_android)
            else -> info.key
        }

        binding.txtSubtitle.text = info.value
    }

    @OnViewRecycled
    fun clear() {
        binding.txtTitle.text = null
        binding.txtSubtitle.text = null
    }
}
