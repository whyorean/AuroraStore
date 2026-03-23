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

package com.aurora.store.view.custom

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import com.aurora.store.R
import com.aurora.store.databinding.ViewRatingBinding

class RatingView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    private lateinit var binding: ViewRatingBinding

    var number = 0
    var max = 0
    var rating = 0

    constructor(context: Context, number: Int, max: Int, rating: Int) : this(context) {
        this.number = number
        this.max = max
        this.rating = rating
        init(context)
    }

    private fun init(context: Context) {
        val view = inflate(context, R.layout.view_rating, this)
        binding = ViewRatingBinding.bind(view)

        binding.avgNum.text = number.toString()
        binding.avgRating.max = max
        binding.avgRating.progress = rating
    }
}
