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
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.RelativeLayout
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.airbnb.epoxy.OnViewRecycled
import com.aurora.gplayapi.data.models.Artwork
import com.aurora.store.GlideApp
import com.aurora.store.R
import com.aurora.store.databinding.ViewScreenshotLargeBinding
import com.aurora.store.util.extensions.clear
import com.aurora.store.util.extensions.px
import com.aurora.store.util.extensions.runOnUiThread
import com.aurora.store.view.epoxy.views.BaseView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

@ModelView(
    autoLayout = ModelView.Size.MATCH_WIDTH_MATCH_HEIGHT,
    baseModelClass = BaseView::class
)
class LargeScreenshotView : RelativeLayout {

    private lateinit var B: ViewScreenshotLargeBinding

    constructor(context: Context?) : super(context) {
        init(context, null)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
    }

    private fun init(context: Context?, attrs: AttributeSet?) {
        val view = inflate(context, R.layout.view_screenshot_large, this)
        B = ViewScreenshotLargeBinding.bind(view)
    }

    @ModelProp
    fun artwork(artwork: Artwork) {
        val displayMetrics = Resources.getSystem().displayMetrics
        GlideApp.with(context)
            .load("${artwork.url}=rw-w${displayMetrics.widthPixels}-v1-e15")
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .addListener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

                override fun onResourceReady(
                    drawable: Drawable,
                    model: Any,
                    target: Target<Drawable?>,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    runOnUiThread {
                        if (artwork.height != 0 && artwork.width != 0) {
                            B.img.layoutParams.height = artwork.height.px.toInt()
                            B.img.layoutParams.width = artwork.width.px.toInt()
                        } else {
                            val displayMetrics = Resources.getSystem().displayMetrics
                            val height = displayMetrics.heightPixels
                            val width = displayMetrics.widthPixels
                            B.img.layoutParams.width = width
                            B.img.layoutParams.height = height
                        }
                        B.img.setImageDrawable(drawable)
                        B.img.requestLayout()
                    }
                    return false
                }
            }).submit()
    }

    @OnViewRecycled
    fun clear() {
        B.img.clear()
    }
}