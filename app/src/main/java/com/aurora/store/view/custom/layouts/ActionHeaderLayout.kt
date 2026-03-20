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
import android.view.View
import android.widget.RelativeLayout
import com.aurora.store.R
import com.aurora.store.databinding.ViewActionHeaderBinding

class ActionHeaderLayout : RelativeLayout {

    private lateinit var binding: ViewActionHeaderBinding

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
        val view = inflate(context, R.layout.view_action_header, this)
        binding = ViewActionHeaderBinding.bind(view)

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ActionHeaderLayout)
        val textTitle = typedArray.getString(R.styleable.ActionHeaderLayout_headerTitle)
        val textSubtitle = typedArray.getString(R.styleable.ActionHeaderLayout_headerSubtitle)

        typedArray.recycle()

        textTitle?.let {
            binding.txtTitle.text = it
        }

        textSubtitle?.let {
            binding.txtSubtitle.visibility = View.VISIBLE
            binding.txtSubtitle.text = it
        }
    }

    fun setTitle(header: String?) {
        binding.txtTitle.text = header
    }

    fun setSubTitle(subHeader: String?) {
        binding.txtSubtitle.visibility = View.VISIBLE
        binding.txtSubtitle.text = subHeader
    }

    fun addClickListener(onclickListener: OnClickListener?) {
        binding.imgAction.visibility = View.VISIBLE
        binding.imgAction.setOnClickListener(onclickListener)
    }
}
