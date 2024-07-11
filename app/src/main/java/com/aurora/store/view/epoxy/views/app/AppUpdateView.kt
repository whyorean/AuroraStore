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

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import coil.load
import coil.transform.CircleCropTransformation
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
import com.aurora.store.view.epoxy.views.BaseModel
import com.aurora.store.view.epoxy.views.BaseView

@ModelView(
    autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT,
    baseModelClass = BaseModel::class
)
class AppUpdateView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseView<ViewAppUpdateBinding>(context, attrs, defStyleAttr) {
    private lateinit var iconDrawable: Drawable

    @ModelProp
    fun app(app: App) {
        /*Inflate App details*/
        with(app) {
            binding.txtLine1.text = displayName
            binding.imgIcon.load(iconArtwork.url) {
                placeholder(R.drawable.bg_placeholder)
                transformations(RoundedCornersTransformation(8.px.toFloat()))
                listener { _, result ->
                    result.drawable.let { iconDrawable = it }
                }
            }

            binding.txtLine2.text = developerName
            binding.txtLine3.text = ("${CommonUtil.addSiPrefix(size)}  •  $updatedOn")
            binding.txtLine4.text = ("$versionName (${versionCode})")
            binding.txtChangelog.text = if (changes.isNotEmpty())
                HtmlCompat.fromHtml(
                    changes,
                    HtmlCompat.FROM_HTML_OPTION_USE_CSS_COLORS
                )
            else
                context.getString(R.string.details_changelog_unavailable)

            binding.headerIndicator.setOnClickListener {
                if (binding.txtChangelog.isVisible) {
                    binding.headerIndicator.icon =
                        ContextCompat.getDrawable(context, R.drawable.ic_arrow_down)
                    binding.txtChangelog.visibility = View.GONE
                } else {
                    binding.headerIndicator.icon =
                        ContextCompat.getDrawable(context, R.drawable.ic_arrow_up)
                    binding.txtChangelog.visibility = View.VISIBLE
                }
            }
        }
    }

    @ModelProp
    fun download(download: Download?) {
        if (download != null) {
            binding.btnAction.updateState(download.downloadStatus)
            binding.progressDownload.isIndeterminate = download.progress < 1
            when (download.downloadStatus) {
                DownloadStatus.QUEUED -> {
                    binding.progressDownload.progress = 0
                    animateImageView(scaleFactor = 0.75f)
                }

                DownloadStatus.DOWNLOADING -> {
                    if (download.progress > 0) {
                        if (download.progress == 100) {
                            binding.progressDownload.invisible()
                        } else {
                            binding.progressDownload.progress = download.progress
                            animateImageView(scaleFactor = 0.75f)
                        }
                    }
                }

                else -> {
                    binding.progressDownload.progress = 0
                    animateImageView()
                }
            }
        }
    }

    @CallbackProp
    fun click(onClickListener: OnClickListener?) {
        binding.layoutContent.setOnClickListener(onClickListener)
    }

    @CallbackProp
    fun positiveAction(onClickListener: OnClickListener?) {
        binding.btnAction.addPositiveOnClickListener(onClickListener)
    }

    @CallbackProp
    fun negativeAction(onClickListener: OnClickListener?) {
        binding.btnAction.addNegativeOnClickListener(onClickListener)
    }

    @CallbackProp
    fun longClick(onClickListener: OnLongClickListener?) {
        binding.layoutContent.setOnLongClickListener(onClickListener)
    }

    @OnViewRecycled
    fun clear() {
        binding.headerIndicator.removeCallbacks { }
        binding.progressDownload.progress = 0
        animateImageView()
    }

    private fun animateImageView(scaleFactor: Float = 1f) {
        val isDownloadVisible = binding.progressDownload.isShown

        if (isDownloadVisible && scaleFactor != 1f)
            return

        if (!isDownloadVisible && scaleFactor == 1f)
            return

        if (scaleFactor == 1f) {
            binding.progressDownload.invisible()
        } else {
            binding.progressDownload.postDelayed({ binding.progressDownload.show() }, 250)
        }

        val scale = listOf(
            ObjectAnimator.ofFloat(binding.imgIcon, "scaleX", scaleFactor),
            ObjectAnimator.ofFloat(binding.imgIcon, "scaleY", scaleFactor)
        )

        scale.forEach { animator ->
            animator.interpolator = AccelerateDecelerateInterpolator()
            animator.duration = 500
            animator.start()
        }

        iconDrawable?.let {
            binding.imgIcon.load(it) {
                transformations(
                    if (scaleFactor == 1f)
                        RoundedCornersTransformation(8.px.toFloat())
                    else
                        CircleCropTransformation()
                )
            }
        }
    }

}
