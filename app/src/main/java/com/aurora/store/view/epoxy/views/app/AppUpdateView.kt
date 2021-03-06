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
import android.widget.RelativeLayout
import androidx.core.text.HtmlCompat
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.airbnb.epoxy.OnViewRecycled
import com.aurora.extensions.invisible
import com.aurora.extensions.load
import com.aurora.extensions.px
import com.aurora.extensions.show
import com.aurora.store.R
import com.aurora.store.State
import com.aurora.store.data.model.UpdateFile
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
    fun updateFile(updateFile: UpdateFile?) {
        if (updateFile != null) {

            /*Inflate App details*/
            with(updateFile.app) {
                B.txtLine1.text = displayName
                B.imgIcon.load(iconArtwork.url, withCrossFade()) {
                    placeholder(R.drawable.bg_placeholder)
                    transform(RoundedCorners(8.px.toInt()))
                }

                B.txtLine2.text = developerName
                B.txtLine3.text = ("${CommonUtil.addSiPrefix(size)}  ???  $updatedOn")
                B.txtLine4.text = ("v${versionName}.${versionCode}")
                B.txtChangelog.text = if (changes.isNotEmpty())
                    HtmlCompat.fromHtml(
                        changes,
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

            /*Inflate Download details*/
            updateFile.group?.let {
                when (updateFile.state) {
                    State.QUEUED -> {
                        B.progressDownload.progress = 0
                        B.progressDownload.show()
                        B.btnAction.updateState(State.QUEUED)
                    }
                    State.IDLE, State.CANCELED -> {
                        B.progressDownload.progress = 0
                        B.progressDownload.invisible()
                        B.btnAction.updateState(State.IDLE)
                    }
                    State.PROGRESS -> {
                        val progress = it.groupDownloadProgress
                        if (progress > 0) {
                            if (progress == 100) {
                                B.progressDownload.invisible()
                            } else {
                                B.progressDownload.progress = progress
                                B.progressDownload.show()
                            }
                        }
                    }
                    State.COMPLETE -> {
                        B.progressDownload.invisible()
                        B.btnAction.updateState(State.COMPLETE)
                    }
                }
            }
        }
    }

    @ModelProp
    fun state(state: State?) {
        state?.let {
            B.btnAction.updateState(it)
        }
    }

    @CallbackProp
    fun click(onClickListener: OnClickListener?) {
        B.layoutContent.setOnClickListener(onClickListener)
    }

    @CallbackProp
    fun positiveAction(onClickListener: OnClickListener?) {
        B.btnAction.addPositiveOnClickListener(onClickListener)
    }

    @CallbackProp
    fun negativeAction(onClickListener: OnClickListener?) {
        B.btnAction.addNegativeOnClickListener(onClickListener)
    }

    @CallbackProp
    fun installAction(onClickListener: OnClickListener?) {
        B.btnAction.addInstallOnClickListener(onClickListener)
    }

    @CallbackProp
    fun longClick(onClickListener: OnLongClickListener?) {
        B.layoutContent.setOnLongClickListener(onClickListener)
    }

    @OnViewRecycled
    fun clear() {
        B.headerIndicator.removeCallbacks { }
        B.progressDownload.invisible()
    }
}