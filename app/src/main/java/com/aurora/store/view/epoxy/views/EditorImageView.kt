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

package com.aurora.store.view.epoxy.views

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.airbnb.epoxy.OnViewRecycled
import com.aurora.gplayapi.data.models.Artwork
import com.aurora.store.R
import com.aurora.store.databinding.ViewEditorImageBinding
import com.aurora.store.util.extensions.clear
import com.aurora.store.util.extensions.load
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

@ModelView(
    autoLayout = ModelView.Size.WRAP_WIDTH_WRAP_HEIGHT,
    baseModelClass = BaseView::class
)
class EditorImageView : RelativeLayout {

    private lateinit var B: ViewEditorImageBinding

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
        val view = inflate(context, R.layout.view_editor_image, this)
        B = ViewEditorImageBinding.bind(view)
    }

    @ModelProp
    fun artwork(artwork: Artwork) {
        when (artwork.type) {
            14 -> {
                B.img.layoutParams.width =
                    resources.getDimension(R.dimen.icon_size_large).toInt() * 2
                B.img.requestLayout()
                B.img.load(artwork.url, DrawableTransitionOptions.withCrossFade()) {
                    transform(RoundedCorners(16))
                }
            }
            else -> {
                B.img.layoutParams.width = resources.getDimension(R.dimen.icon_size_large).toInt()
                B.img.requestLayout()
                B.img.load(artwork.url, DrawableTransitionOptions.withCrossFade()) {
                    transform(RoundedCorners(16))
                }
            }
        }
    }

    @OnViewRecycled
    fun clear() {
        B.img.clear()
    }
}