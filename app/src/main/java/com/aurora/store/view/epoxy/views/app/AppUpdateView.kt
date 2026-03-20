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
import coil3.asDrawable
import coil3.load
import coil3.request.placeholder
import coil3.request.transformations
import coil3.transform.CircleCropTransformation
import coil3.transform.RoundedCornersTransformation
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.airbnb.epoxy.OnViewRecycled
import com.aurora.extensions.invisible
import com.aurora.extensions.px
import com.aurora.store.R
import com.aurora.store.data.model.DownloadStatus
import com.aurora.store.data.room.download.Download
import com.aurora.store.data.room.update.Update
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
    private var iconDrawable: Drawable? = null
    private val cornersTransformation = RoundedCornersTransformation(8.px.toFloat())

    @ModelProp
    fun update(update: Update) {
        /*Inflate App details*/
        with(update) {
            binding.txtLine1.text = displayName
            binding.imgIcon.load(iconURL) {
                placeholder(R.drawable.bg_placeholder)
                transformations(cornersTransformation)
                listener { _, result ->
                    result.image.asDrawable(resources).let { iconDrawable = it }
                }
            }

            binding.txtLine2.text = developerName
            binding.txtLine3.text = ("${CommonUtil.addSiPrefix(size)}  •  $updatedOn")
            binding.txtLine4.text = ("$versionName ($versionCode)")
            binding.txtChangelog.text = if (changelog.isNotEmpty()) {
                HtmlCompat.fromHtml(
                    changelog,
                    HtmlCompat.FROM_HTML_OPTION_USE_CSS_COLORS
                )
            } else {
                context.getString(R.string.details_changelog_unavailable)
            }

            binding.headerIndicator.setOnClickListener {
                if (binding.cardChangelog.isVisible) {
                    binding.headerIndicator.icon =
                        ContextCompat.getDrawable(context, R.drawable.ic_arrow_down)
                    binding.cardChangelog.visibility = View.GONE
                } else {
                    binding.headerIndicator.icon =
                        ContextCompat.getDrawable(context, R.drawable.ic_arrow_up)
                    binding.cardChangelog.visibility = View.VISIBLE
                }
            }
        }
    }

    @ModelProp
    fun download(download: Download?) {
        if (download != null) {
            binding.btnAction.updateState(download.status)
            when (download.status) {
                DownloadStatus.VERIFYING,
                DownloadStatus.QUEUED -> {
                    binding.progressDownload.isIndeterminate = true
                    animateImageView(scaleFactor = 0.75f)
                }

                DownloadStatus.DOWNLOADING -> {
                    binding.progressDownload.isIndeterminate = false
                    binding.progressDownload.progress = download.progress
                    animateImageView(scaleFactor = 0.75f)
                }

                else -> {
                    binding.progressDownload.isIndeterminate = true
                    animateImageView(scaleFactor = 1f)
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
        iconDrawable = null

        binding.apply {
            headerIndicator.removeCallbacks {}
            progressDownload.invisible()
            btnAction.apply {
                removeCallbacks { }
                updateState(DownloadStatus.UNAVAILABLE)
            }
        }
    }

    private fun animateImageView(scaleFactor: Float = 1f) {
        val isDownloadVisible = binding.progressDownload.isShown

        // Avoids flickering when the download is in progress
        if (isDownloadVisible && scaleFactor != 1f) {
            return
        }

        if (!isDownloadVisible && scaleFactor == 1f) {
            return
        }

        if (scaleFactor == 1f) {
            binding.progressDownload.invisible()
        } else {
            binding.progressDownload.show()
        }

        val scale = listOf(
            ObjectAnimator.ofFloat(binding.imgIcon, "scaleX", scaleFactor),
            ObjectAnimator.ofFloat(binding.imgIcon, "scaleY", scaleFactor)
        )

        scale.forEach { animation ->
            animation.apply {
                interpolator = AccelerateDecelerateInterpolator()
                duration = 250
                start()
            }
        }

        iconDrawable?.let {
            binding.imgIcon.load(it) {
                transformations(
                    if (scaleFactor == 1f) {
                        cornersTransformation
                    } else {
                        CircleCropTransformation()
                    }
                )
            }
        }
    }
}
