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
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import coil.load
import coil.transform.RoundedCornersTransformation
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.airbnb.epoxy.OnViewRecycled
import com.aurora.extensions.invisible
import com.aurora.extensions.px
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.data.model.DownloadStatus
import com.aurora.store.data.room.download.Download
import com.aurora.store.databinding.ViewAppUpdateBinding
import com.aurora.store.util.CommonUtil
import com.aurora.store.util.PathUtil
import com.aurora.store.view.epoxy.views.BaseView
import java.io.File

@ModelView(
    autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT,
    baseModelClass = BaseView::class
)
class AppUpdateView : RelativeLayout {

    private lateinit var B: ViewAppUpdateBinding

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
        val view = inflate(context, R.layout.view_app_update, this)
        B = ViewAppUpdateBinding.bind(view)

        B.progressDownload.progressDrawable?.apply {
            val alphaColor = ColorUtils.setAlphaComponent(Color.GRAY, 100)
            colorFilter = PorterDuffColorFilter(alphaColor, PorterDuff.Mode.SRC_IN)
        }
    }

    @ModelProp
    fun app(app: App) {
        /*Inflate App details*/
        with(app) {
            B.txtLine1.text = displayName
            B.imgIcon.load(iconArtwork.url) {
                placeholder(R.drawable.bg_placeholder)
                transformations(RoundedCornersTransformation(8.px.toFloat()))
            }

            B.txtLine2.text = developerName
            B.txtLine3.text = ("${CommonUtil.addSiPrefix(size)}  •  $updatedOn")
            B.txtLine4.text = ("$versionName (${versionCode})")
            B.txtChangelog.text = if (changes.isNotEmpty())
                HtmlCompat.fromHtml(
                    changes,
                    HtmlCompat.FROM_HTML_OPTION_USE_CSS_COLORS
                )
            else
                context.getString(R.string.details_changelog_unavailable)

            B.headerIndicator.setOnClickListener {
                if (B.txtChangelog.isVisible) {
                    B.headerIndicator.icon = ContextCompat.getDrawable(context, R.drawable.ic_arrow_down)
                    B.txtChangelog.visibility = View.GONE
                } else {
                    B.headerIndicator.icon = ContextCompat.getDrawable(context, R.drawable.ic_arrow_up)
                    B.txtChangelog.visibility = View.VISIBLE
                }
            }
        }
    }

    @ModelProp
    fun download(download: Download?) {
        if (download != null) {
            /*Inflate Download details*/
            B.btnAction.updateState(download.status)
            when (download.status) {
                DownloadStatus.QUEUED -> {
                    B.progressDownload.progress = 0
                    B.progressDownload.show()
                }
                DownloadStatus.DOWNLOADING -> {
                    if (download.progress > 0) {
                        if (download.progress == 100) {
                            B.progressDownload.invisible()
                        } else {
                            B.progressDownload.progress = download.progress
                            B.progressDownload.show()
                        }
                    }
                }
                DownloadStatus.COMPLETED -> {
                    B.progressDownload.invisible()

                    // It is possible that while the app was downloaded in past, files have been removed
                    // Check once again to reflect appropriate status
                    val files = File(
                        PathUtil.getAppDownloadDir(
                            context,
                            download.packageName,
                            download.versionCode
                        ).path
                    ).listFiles()
                    if (files.isNullOrEmpty()) B.btnAction.updateState(DownloadStatus.UNAVAILABLE)
                }
                else -> {
                    B.progressDownload.progress = 0
                    B.progressDownload.invisible()
                }
            }
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
        B.progressDownload.progress = 0
        B.progressDownload.invisible()
    }
}
