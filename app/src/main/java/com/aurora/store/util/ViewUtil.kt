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

package com.aurora.store.util

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityOptionsCompat


object ViewUtil {

    fun getEmptyActivityBundle(context: Context?): Bundle? {
        return ActivityOptionsCompat.makeCustomAnimation(
            context!!,
            android.R.anim.fade_in,
            android.R.anim.fade_out
        ).toBundle()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun configureActivityLayout(activity: Activity, isLight: Boolean) {
        val window = activity.window
        val params = window.attributes
        params.flags = params.flags and WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
        window.attributes = params
        window.statusBarColor = Color.TRANSPARENT
        setFullScreenLightStatusBar(activity, isLight)
    }

    private fun setFullScreenLightStatusBar(activity: Activity, isLight: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var flags = activity.window.decorView.systemUiVisibility
            if (isLight)
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            flags = flags or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            flags = flags or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            activity.window.decorView.systemUiVisibility = flags
        }
    }

    fun getStyledAttribute(context: Context, styleID: Int): Int {
        val arr = context.obtainStyledAttributes(TypedValue().data, intArrayOf(styleID))
        val styledColor = arr.getColor(0, Color.WHITE)
        arr.recycle()
        return styledColor
    }
}