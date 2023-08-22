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
import com.aurora.extensions.toDate
import com.aurora.gplayapi.data.models.Review
import com.aurora.store.R
import com.aurora.store.databinding.ViewReviewBinding
import com.aurora.store.view.epoxy.views.BaseView

@ModelView(
    autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT,
    baseModelClass = BaseView::class
)
class ReviewView : RelativeLayout {

    private lateinit var B: ViewReviewBinding

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
        val view = inflate(context, R.layout.view_review, this)
        B = ViewReviewBinding.bind(view)
    }

    @ModelProp
    fun review(review: Review) {
        B.txtAuthor.text = review.userName
        B.txtTime.text = ("${review.timeStamp.toDate()}  •  v${review.appVersion}")
        B.txtComment.text = review.comment

        B.img.load(review.userPhotoUrl) {
            placeholder(R.drawable.bg_placeholder)
            transformations(RoundedCornersTransformation(32F))
        }

        B.rating.rating = review.rating.toFloat()
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            B.line2.justificationMode = Layout.JUSTIFICATION_MODE_INTER_WORD
        }*/
    }

    @CallbackProp
    fun click(onClickListener: OnClickListener?) {
        B.root.setOnClickListener(onClickListener)
    }

    @CallbackProp
    fun longClick(onClickListener: OnLongClickListener?) {
        B.root.setOnLongClickListener(onClickListener)
    }
}
