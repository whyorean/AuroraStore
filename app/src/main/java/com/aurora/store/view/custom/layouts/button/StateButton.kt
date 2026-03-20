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

package com.aurora.store.view.custom.layouts.button

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import com.aurora.store.R
import com.aurora.store.databinding.ViewStateButtonBinding

class StateButton : RelativeLayout {

    private lateinit var binding: ViewStateButtonBinding

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
        val view = inflate(context, R.layout.view_state_button, this)
        binding = ViewStateButtonBinding.bind(view)

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.StateButton)
        val btnTxt = typedArray.getString(R.styleable.StateButton_btnStateText)
        val btnIcon = typedArray.getResourceId(
            R.styleable.StateButton_btnStateIcon,
            R.drawable.ic_arrow_right
        )

        binding.btn.text = btnTxt
        binding.btn.icon = ContextCompat.getDrawable(context, btnIcon)
        typedArray.recycle()
    }

    fun updateProgress(isVisible: Boolean) {
        if (isVisible)
            binding.progress.visibility = View.VISIBLE
        else

            binding.progress.visibility = View.INVISIBLE
    }

    fun addOnClickListener(onClickListener: OnClickListener) {
        binding.btn.setOnClickListener(onClickListener)
    }
}
