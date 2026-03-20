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

package com.aurora.store.view.epoxy.views.preference

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.aurora.store.R
import com.aurora.store.data.model.Permission
import com.aurora.store.databinding.ViewPermissionBinding
import com.aurora.store.view.epoxy.views.BaseModel
import com.aurora.store.view.epoxy.views.BaseView

@ModelView(
    autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT,
    baseModelClass = BaseModel::class
)
class PermissionView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseView<ViewPermissionBinding>(context, attrs, defStyleAttr) {

    @ModelProp
    fun permission(installer: Permission) {
        binding.line1.text = installer.title
        binding.line2.text = installer.subtitle
    }

    @ModelProp
    fun isGranted(granted: Boolean) {
        if (granted) {
            binding.btnAction.isEnabled = false
            binding.btnAction.text = ContextCompat.getString(context, R.string.action_granted)
        } else {
            binding.btnAction.isEnabled = true
            binding.btnAction.text = ContextCompat.getString(context, R.string.action_grant)
        }
    }

    @CallbackProp
    fun click(onClickListener: OnClickListener?) {
        binding.btnAction.setOnClickListener(onClickListener)
    }
}
