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
import android.widget.CompoundButton
import android.widget.RelativeLayout
import coil.load
import coil.transform.RoundedCornersTransformation
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.aurora.store.R
import com.aurora.store.data.model.Black
import com.aurora.store.databinding.ViewBlackBinding

@ModelView(
    autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT,
    baseModelClass = BaseView::class
)
class BlackListView : RelativeLayout {

    private lateinit var B: ViewBlackBinding

    constructor(context: Context?) : super(context) {
        init(context)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context)
    }

    private fun init(context: Context?) {
        val view = inflate(context, R.layout.view_black, this)
        B = ViewBlackBinding.bind(view)
    }

    @ModelProp
    fun black(black: Black) {
        B.imgIcon.load(black.drawable) {
            placeholder(R.drawable.bg_placeholder)
            transformations(RoundedCornersTransformation(25F))
        }

        B.txtLine1.text = black.displayName
        B.txtLine2.text = black.packageName
        B.txtLine3.text = ("${black.versionName}.${black.versionCode}")
    }

    @ModelProp
    fun markChecked(isChecked: Boolean) {
        B.checkbox.isChecked = isChecked
    }

    @CallbackProp
    fun checked(onCheckedChangeListener: CompoundButton.OnCheckedChangeListener?) {
        B.checkbox.setOnCheckedChangeListener(onCheckedChangeListener)
    }

    @CallbackProp
    fun click(onClickListener: OnClickListener?) {
        B.root.setOnClickListener(onClickListener)
    }

    @CallbackProp
    fun longClick(onClickListener: OnLongClickListener?) {
        B.root.setOnLongClickListener(onClickListener)
    }
}
