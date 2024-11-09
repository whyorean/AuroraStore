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
import coil3.load
import coil3.request.placeholder
import coil3.request.transformations
import coil3.transform.RoundedCornersTransformation
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.aurora.extensions.px
import com.aurora.gplayapi.data.models.Artwork
import com.aurora.store.R
import com.aurora.store.databinding.ViewScreenshotMiniBinding
import com.aurora.store.view.epoxy.views.BaseModel
import com.aurora.store.view.epoxy.views.BaseView

@ModelView(
    autoLayout = ModelView.Size.WRAP_WIDTH_WRAP_HEIGHT,
    baseModelClass = BaseModel::class
)
class MiniScreenshotView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseView<ViewScreenshotMiniBinding>(context, attrs, defStyleAttr) {

    private var position: Int = 0

    interface ScreenshotCallback {
        fun onClick(position: Int = 0)
    }

    @ModelProp
    fun position(pos: Int) {
        position = pos
    }

    @ModelProp
    fun artwork(artwork: Artwork) {
        normalizeSize(artwork)
        binding.img.load("${artwork.url}=rw-w480-v1-e15") {
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
                    normalizedHeight = 120f
                    normalizedWidth = 120f
                }

                else -> {
                    val factor = artworkHeight / 120f
                    normalizedHeight = 120f
                    normalizedWidth = (artworkWidth / factor)
                }
            }

            binding.img.layoutParams.height = normalizedHeight.px.toInt()
            binding.img.layoutParams.width = normalizedWidth.px.toInt()
            binding.img.requestLayout()
        }
    }

    @CallbackProp
    fun callback(screenshotCallback: ScreenshotCallback?) {
        binding.img.setOnClickListener {
            screenshotCallback?.onClick(position)
        }
    }
}
