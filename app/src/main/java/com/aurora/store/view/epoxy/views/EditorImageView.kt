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
import coil3.load
import coil3.request.transformations
import coil3.transform.RoundedCornersTransformation
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.aurora.extensions.px
import com.aurora.gplayapi.data.models.Artwork
import com.aurora.store.databinding.ViewEditorImageBinding

@ModelView(
    autoLayout = ModelView.Size.WRAP_WIDTH_WRAP_HEIGHT,
    baseModelClass = BaseModel::class
)
class EditorImageView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseView<ViewEditorImageBinding>(context, attrs, defStyleAttr) {

    @ModelProp
    fun artwork(artwork: Artwork) {
        when (artwork.type) {
            14 -> {
                binding.img.layoutParams.height = 108.px.toInt()
                binding.img.layoutParams.width = 192.px.toInt()
                binding.img.requestLayout()

                binding.img.load(artwork.url) {
                    transformations(RoundedCornersTransformation(8.px.toFloat()))
                }
            }

            else -> {
                binding.img.layoutParams.width = 24.px.toInt()
                binding.img.layoutParams.height = 24.px.toInt()
                binding.img.requestLayout()
                binding.img.load(artwork.url) {
                    transformations(RoundedCornersTransformation(4.px.toFloat()))
                }
            }
        }
    }

    @CallbackProp
    fun click(onClickListener: OnClickListener?) {
        binding.root.setOnClickListener(onClickListener)
    }
}
