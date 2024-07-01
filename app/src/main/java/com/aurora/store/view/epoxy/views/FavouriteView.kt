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
import coil.load
import coil.transform.RoundedCornersTransformation
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.data.room.favourites.Favourite
import com.aurora.store.databinding.ViewFavBinding

@ModelView(
    autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT,
    baseModelClass = BaseView::class
)
class FavouriteView : RelativeLayout {

    private lateinit var binding: ViewFavBinding

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
        val view = inflate(context, R.layout.view_fav, this)
        binding = ViewFavBinding.bind(view)
    }

    @ModelProp
    fun favourite(favourite: Favourite) {
        binding.imgIcon.load(favourite.iconURL) {
            placeholder(R.drawable.bg_placeholder)
            transformations(RoundedCornersTransformation(25F))
        }

        binding.txtLine1.text = favourite.displayName
        binding.txtLine2.text = favourite.packageName
    }

    @CallbackProp
    fun onClick(onClickListener: OnClickListener?) {
        binding.root.setOnClickListener(onClickListener)
    }

    @CallbackProp
    fun onFavourite(onClickListener: OnClickListener?) {
        binding.btnFavourite.setOnClickListener(onClickListener)
    }
}
