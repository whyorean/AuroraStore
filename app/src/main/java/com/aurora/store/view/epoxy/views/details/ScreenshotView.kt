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
import android.util.AttributeSet
import android.widget.RelativeLayout
import coil.load
import coil.transform.RoundedCornersTransformation
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.aurora.extensions.px
import com.aurora.gplayapi.data.models.Artwork
import com.aurora.store.R
import com.aurora.store.databinding.ViewScreenshotBinding
import com.aurora.store.view.epoxy.views.BaseView

@ModelView(
    autoLayout = ModelView.Size.WRAP_WIDTH_WRAP_HEIGHT,
    baseModelClass = BaseView::class
)
class ScreenshotView : RelativeLayout {

    private lateinit var B: ViewScreenshotBinding

    private var position: Int = 0

    interface ScreenshotCallback {
        fun onClick(position: Int = 0)
    }

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
        val view = inflate(context, R.layout.view_screenshot, this)
        B = ViewScreenshotBinding.bind(view)
    }

    @ModelProp
    fun position(pos: Int) {
        position = pos
    }

    @ModelProp
    fun artwork(artwork: Artwork) {
        normalizeSize(artwork)
        B.img.load("${artwork.url}=rw-w480-v1-e15") {
            placeholder(R.drawable.bg_rounded)
            transformations(RoundedCornersTransformation(8.px.toFloat()))
        }
    }

    private fun normalizeSize(artwork: Artwork) {
        if (artwork.height != 0 && artwork.width != 0) {

            val artworkHeight = artwork.height
            val artworkWidth = artwork.width

            val normalizedHeight: Float
            val normalizedWidth: Float

            when {
                artworkHeight == artworkWidth -> {
                    normalizedHeight = 192f
                    normalizedWidth = 192f
                }
                else -> {
                    val factor = artworkHeight / 192f
                    normalizedHeight = 192f
                    normalizedWidth = (artworkWidth / factor)
                }
            }

            B.img.layoutParams.height = normalizedHeight.px.toInt()
            B.img.layoutParams.width = normalizedWidth.px.toInt()
            B.img.requestLayout()
        }
    }

    @CallbackProp
    fun callback(screenshotCallback: ScreenshotCallback?) {
        B.img.setOnClickListener {
            screenshotCallback?.onClick(position)
        }
    }
}
