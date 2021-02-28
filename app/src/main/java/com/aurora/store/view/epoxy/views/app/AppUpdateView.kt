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

package com.aurora.store.view.epoxy.views.app

import android.content.Context
import android.util.AttributeSet
import android.widget.CompoundButton
import android.widget.RelativeLayout
import androidx.core.text.HtmlCompat
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.airbnb.epoxy.OnViewRecycled
import com.aurora.extensions.clear
import com.aurora.extensions.load
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.databinding.ViewAppUpdateBinding
import com.aurora.store.util.CommonUtil
import com.aurora.store.view.epoxy.views.BaseView
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade

@ModelView(
    autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT,
    baseModelClass = BaseView::class
)
class AppUpdateView : RelativeLayout {

    private lateinit var B: ViewAppUpdateBinding
    private var expanded: Boolean = false

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
        val view = inflate(context, R.layout.view_app_update, this)
        B = ViewAppUpdateBinding.bind(view)
    }

    @ModelProp
    fun app(app: App) {
        B.txtLine1.text = app.displayName
        B.imgIcon.load(app.iconArtwork.url, withCrossFade()) {
            placeholder(R.drawable.bg_placeholder)
            transform(RoundedCorners(25))
        }

        B.txtLine2.text = app.developerName
        B.txtLine3.text = ("${CommonUtil.addSiPrefix(app.size)}  •  ${app.updatedOn}")
        B.txtLine4.text = ("v${app.versionName}.${app.versionCode}")
        B.txtChangelog.text = if (app.changes.isNotEmpty())
            HtmlCompat.fromHtml(
                app.changes,
                HtmlCompat.FROM_HTML_OPTION_USE_CSS_COLORS
            )
        else
            context.getString(R.string.details_changelog_unavailable)

        B.headerIndicator.setOnClickListener {
            B.expansionLayout.let {
                if (it.isExpanded) {
                    it.collapse(true)
                } else {
                    it.expand(true)
                }
            }
        }
    }

    @ModelProp
    fun markChecked(isChecked: Boolean) {
        B.checkbox.isChecked = isChecked
    }

    @CallbackProp
    fun checked(onCheckedChangeListener: CompoundButton.OnCheckedChangeListener?) {
        B.checkbox.setOnCheckedChangeListener(onCheckedChangeListener)
    }

    @CallbackProp
    fun click(onClickListener: OnClickListener?) {
        B.layoutContent.setOnClickListener(onClickListener)
    }

    @CallbackProp
    fun longClick(onClickListener: OnLongClickListener?) {
        B.layoutContent.setOnLongClickListener(onClickListener)
    }

    @OnViewRecycled
    fun clear() {
        B.imgIcon.clear()
        B.headerIndicator.removeCallbacks { }
    }
}