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
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import com.aurora.store.R
import com.aurora.store.data.model.State
import com.aurora.store.databinding.ViewActionButtonBinding

class ActionButton : RelativeLayout {

    private lateinit var binding: ViewActionButtonBinding

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
        val view = inflate(context, R.layout.view_action_button, this)
        binding = ViewActionButtonBinding.bind(view)

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ActionButton)
        val btnTxt = typedArray.getString(R.styleable.ActionButton_btnActionText)

        val btnTxtColor = typedArray.getResourceId(
            R.styleable.ActionButton_btnActionTextColor,
            R.color.colorWhite
        )

        val stateIcon = typedArray.getResourceId(
            R.styleable.ActionButton_btnActionIcon,
            R.drawable.ic_check
        )

        val stateColor = ContextCompat.getColor(context, btnTxtColor)

        binding.btn.text = btnTxt
        binding.btn.setTextColor(stateColor)
        binding.img.setImageDrawable(ContextCompat.getDrawable(context, stateIcon))
        binding.img.imageTintList = ColorStateList.valueOf(stateColor)

        typedArray.recycle()
    }

    fun setText(text: String) {
        binding.viewFlipper.displayedChild = 0
        binding.btn.text = text
    }

    fun setText(text: Int) {
        binding.viewFlipper.displayedChild = 0
        binding.btn.text = ContextCompat.getString(context, text)
    }

    fun setButtonState(enabled: Boolean = true) {
        binding.btn.isEnabled = enabled
    }

    fun updateState(state: State) {
        val displayChild = when (state) {
            State.PROGRESS -> 1
            State.COMPLETE -> 2
            else -> 0
        }

        if (binding.viewFlipper.displayedChild != displayChild) {
            binding.viewFlipper.displayedChild = displayChild

            if (displayChild == 2) updateState(State.IDLE)
        }
    }

    fun addOnClickListener(onClickListener: OnClickListener?) {
        binding.btn.setOnClickListener(onClickListener)
    }
}
