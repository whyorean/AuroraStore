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

package com.aurora.store.view.custom.layouts

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isVisible
import com.aurora.store.R
import com.aurora.store.databinding.ViewDevInfoBinding

class DevInfoLayout : RelativeLayout {

    private lateinit var binding: ViewDevInfoBinding

    val icon: AppCompatImageView get() = binding.img

    var title: String
        get() = binding.txtTitle.text.toString()
        set(value) = setTxtTitle(value)

    var subTitle: String?
        get() = binding.txtSubtitle.text.toString()
        set(value) = setTxtSubtitle(value)

    @get:ColorInt
    var titleColor: Int
        get() = binding.txtTitle.currentTextColor
        set(value) = binding.txtTitle.setTextColor(value)

    @get:ColorInt
    var subTitleColor: Int
        get() = binding.txtSubtitle.currentTextColor
        set(value) = binding.txtSubtitle.setTextColor(value)

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        val view = inflate(context, R.layout.view_dev_info, this)
        binding = ViewDevInfoBinding.bind(view)

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.DevInfoLayout)
        val icon = typedArray.getResourceId(
            R.styleable.DevInfoLayout_imgIcon,
            R.drawable.ic_map_marker
        )

        val textPrimary = typedArray.getString(R.styleable.DevInfoLayout_txtTitle)
        val textSecondary = typedArray.getString(R.styleable.DevInfoLayout_txtSubtitle)

        binding.img.setImageResource(icon)
        binding.txtTitle.text = textPrimary
        binding.txtSubtitle.text = textSecondary
        typedArray.recycle()
    }

    private fun setTxtTitle(text: String?) {
        binding.txtTitle.text = text
        binding.txtTitle.isVisible = text != null
        invalidate()
    }

    private fun setTxtSubtitle(text: String?) {
        binding.txtSubtitle.text = text
        binding.txtSubtitle.isVisible = text != null
        invalidate()
    }
}
