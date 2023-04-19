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
import android.os.Build
import android.util.AttributeSet
import android.widget.RelativeLayout
import androidx.annotation.RequiresApi
import com.aurora.extensions.getString
import com.aurora.extensions.runOnUiThread
import com.aurora.store.R
import com.aurora.store.State
import com.aurora.store.databinding.ViewUpdateButtonBinding

class UpdateButton : RelativeLayout {

    private lateinit var B: ViewUpdateButtonBinding

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context)
    }

    private fun init(context: Context) {
        val view = inflate(context, R.layout.view_update_button, this)
        B = ViewUpdateButtonBinding.bind(view)
    }

    fun setText(text: String) {
        B.viewFlipper.displayedChild = 0
        B.btnPositive.text = text
    }

    fun setText(text: Int) {
        B.viewFlipper.displayedChild = 0
        B.btnPositive.text = getString(text)
    }

    fun updateState(state: State) {
        val displayChild = when (state) {
            State.IDLE, State.CANCELED -> 0
            State.QUEUED -> 1
            State.PROGRESS -> 2
            State.COMPLETE -> 3
        }

        if (B.viewFlipper.displayedChild != displayChild) {
            runOnUiThread {
                B.viewFlipper.displayedChild = displayChild
            }
        }
    }

    fun addPositiveOnClickListener(onClickListener: OnClickListener?) {
        B.btnPositive.setOnClickListener(onClickListener)
    }

    fun addNegativeOnClickListener(onClickListener: OnClickListener?) {
        B.btnNegative.setOnClickListener(onClickListener)
    }

    fun addInstallOnClickListener(onClickListener: OnClickListener?) {
        B.btnInstall.setOnClickListener(onClickListener)
    }
}
