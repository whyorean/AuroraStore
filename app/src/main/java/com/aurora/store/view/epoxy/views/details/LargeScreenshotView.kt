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

package com.aurora.store.view.epoxy.views.details

import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.widget.RelativeLayout
import coil.load
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.aurora.gplayapi.data.models.Artwork
import com.aurora.store.R
import com.aurora.store.databinding.ViewScreenshotLargeBinding
import com.aurora.store.view.epoxy.views.BaseView

@ModelView(
    autoLayout = ModelView.Size.MATCH_WIDTH_MATCH_HEIGHT,
    baseModelClass = BaseView::class
)
class LargeScreenshotView : RelativeLayout {

    private lateinit var B: ViewScreenshotLargeBinding

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
        val view = inflate(context, R.layout.view_screenshot_large, this)
        B = ViewScreenshotLargeBinding.bind(view)
    }

    @ModelProp
    fun artwork(artwork: Artwork) {
        val displayMetrics = Resources.getSystem().displayMetrics
        B.img.load("${artwork.url}=rw-w${displayMetrics.widthPixels}-v1-e15") {
            placeholder(R.drawable.bg_placeholder)
        }
    }
}
