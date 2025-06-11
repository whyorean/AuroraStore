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
import android.util.TypedValue
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.aurora.store.databinding.ViewTextBinding

@ModelView(
    autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT,
    baseModelClass = BaseModel::class
)
class EpoxyTextView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseView<ViewTextBinding>(context, attrs, defStyleAttr) {

    @ModelProp
    fun title(title: String) {
        binding.txtView.text = title
    }

    @ModelProp
    fun size(int: Int) {
        binding.txtView.setTextSize(TypedValue.COMPLEX_UNIT_SP, int.toFloat())
    }

    @ModelProp
    fun style(resId: Int) {
        binding.txtView.setTextAppearance(context, resId)
    }
}
