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

package com.aurora.store.view.custom

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import androidx.annotation.RequiresApi
import com.aurora.store.R
import com.aurora.store.databinding.ViewActionButtonBinding

class ActionButton : RelativeLayout {

    private lateinit var B: ViewActionButtonBinding

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

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        val view = inflate(context, R.layout.view_action_button, this)
        B = ViewActionButtonBinding.bind(view)

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ActionButton)
        val btnTxt = typedArray.getString(R.styleable.ActionButton_btnActionText)
        B.btn.text = btnTxt
        typedArray.recycle()
    }

    fun setText(text: String) {
        B.btn.text = text
    }

    fun updateProgress(isVisible: Boolean) {
        if (isVisible)
            B.progress.visibility = View.VISIBLE
        else

            B.progress.visibility = View.INVISIBLE
    }

    fun addOnClickListener(onClickListener: OnClickListener) {
        B.btn.setOnClickListener(onClickListener)
    }
}