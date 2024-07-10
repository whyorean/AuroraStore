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
import androidx.core.content.ContextCompat
import coil.load
import coil.transform.RoundedCornersTransformation
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.aurora.gplayapi.SearchSuggestEntry
import com.aurora.store.R
import com.aurora.store.databinding.ViewSearchSuggestionBinding

@ModelView(
    autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT,
    baseModelClass = BaseModel::class
)
class SearchSuggestionView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseView<ViewSearchSuggestionBinding>(context, attrs, defStyleAttr) {

    @ModelProp
    fun entry(searchSuggestEntry: SearchSuggestEntry) {
        if (searchSuggestEntry.hasImageContainer()) {
            binding.img.load(searchSuggestEntry.imageContainer.imageUrl) {
                transformations(RoundedCornersTransformation(8F))
            }
        } else {
            binding.img.setImageDrawable(
                ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_search_suggestion
                )
            )
        }

        binding.txtTitle.text = searchSuggestEntry.title
    }

    @CallbackProp
    fun click(onClickListener: OnClickListener?) {
        binding.root.setOnClickListener(onClickListener)
    }

    @CallbackProp
    fun action(onClickListener: OnClickListener?) {
        binding.action.setOnClickListener(onClickListener)
    }
}
