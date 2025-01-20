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

package com.aurora.store.view.epoxy.views.app

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import coil3.load
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.aurora.store.databinding.ViewNoAppBinding
import com.aurora.store.view.epoxy.views.BaseModel
import com.aurora.store.view.epoxy.views.BaseView

@ModelView(
    autoLayout = ModelView.Size.MATCH_WIDTH_MATCH_HEIGHT,
    baseModelClass = BaseModel::class
)
class NoAppView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseView<ViewNoAppBinding>(context, attrs, defStyleAttr) {

    @ModelProp
    fun message(@StringRes message: Int) {
        binding.txt.text = context.getString(message)
    }

    @ModelProp
    fun icon(@DrawableRes icon: Int) {
        binding.img.load(icon)
    }

    @JvmOverloads
    @ModelProp
    fun showAction(visibility: Boolean = false) {
        binding.button.isVisible = visibility
    }

    @JvmOverloads
    @ModelProp
    fun actionMessage(@StringRes message: Int? = null) {
        message?.let { binding.button.text = context.getString(message) }
    }

    @JvmOverloads
    @CallbackProp
    fun actionCallback(viewOnClickListener: OnClickListener? = null) {
        binding.button.setOnClickListener(viewOnClickListener)
    }
}
