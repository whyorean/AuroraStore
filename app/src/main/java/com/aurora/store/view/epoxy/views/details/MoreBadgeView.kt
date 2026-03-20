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
import androidx.core.text.HtmlCompat
import coil3.load
import coil3.request.placeholder
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.aurora.gplayapi.data.models.details.Badge
import com.aurora.store.R
import com.aurora.store.databinding.ViewMoreBadgeBinding
import com.aurora.store.view.epoxy.views.BaseModel
import com.aurora.store.view.epoxy.views.BaseView

@ModelView(
    autoLayout = ModelView.Size.WRAP_WIDTH_WRAP_HEIGHT,
    baseModelClass = BaseModel::class
)
class MoreBadgeView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseView<ViewMoreBadgeBinding>(context, attrs, defStyleAttr) {

    @ModelProp
    fun badge(badge: Badge) {
        binding.line1.text = badge.textMajor

        badge.textMinorHtml?.let {
            if (it.isNotEmpty()) {
                binding.line2.text = HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_COMPACT)
            } else {
                binding.line2.text = badge.textMinor
            }
        }

        badge.textDescription?.let {
            if (it.isNotEmpty()) {
                binding.line2.text = it
            }
        }

        badge.artwork?.let {
            binding.img.load(it.url) {
                placeholder(R.drawable.ic_arrow_right)
            }
        }
    }
}
