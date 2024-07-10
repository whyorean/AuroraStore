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
import coil.load
import coil.transform.RoundedCornersTransformation
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.aurora.store.R
import com.aurora.store.data.model.DownloadStatus
import com.aurora.store.data.room.download.Download
import com.aurora.store.databinding.ViewDownloadBinding
import com.aurora.store.util.CommonUtil.getDownloadSpeedString
import com.aurora.store.util.CommonUtil.getETAString
import java.util.Locale

@ModelView(
    autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT,
    baseModelClass = BaseModel::class
)
class DownloadView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseView<ViewDownloadBinding>(context, attrs, defStyleAttr) {

    @ModelProp
    fun download(download: Download) {
        binding.imgDownload.load(download.iconURL) {
            placeholder(R.drawable.bg_placeholder)
            transformations(RoundedCornersTransformation(32F))
        }
        binding.txtTitle.text = download.displayName

        binding.txtStatus.text = download.downloadStatus.name
            .lowercase(Locale.getDefault())
            .replaceFirstChar {
                if (it.isLowerCase()) {
                    it.titlecase(Locale.getDefault())
                } else {
                    it.toString()
                }
            }

        binding.progressDownload.apply {
            progress = download.progress
            isIndeterminate = download.progress <= 0 && !download.isFinished
        }
        binding.txtProgress.text = ("${download.progress}%")

        binding.txtEta.text = getETAString(context, download.timeRemaining)
        binding.txtSpeed.text = getDownloadSpeedString(
            context,
            download.speed
        )

        when (download.downloadStatus) {
            DownloadStatus.DOWNLOADING, DownloadStatus.QUEUED -> {
                binding.txtSpeed.visibility = VISIBLE
                binding.txtEta.visibility = VISIBLE
            }

            else -> {
                binding.txtSpeed.visibility = INVISIBLE
                binding.txtEta.visibility = INVISIBLE
            }
        }
    }

    @CallbackProp
    fun click(onClickListener: OnClickListener?) {
        binding.root.setOnClickListener(onClickListener)
    }

    @CallbackProp
    fun longClick(onClickListener: OnLongClickListener?) {
        binding.root.setOnLongClickListener(onClickListener)
    }
}
