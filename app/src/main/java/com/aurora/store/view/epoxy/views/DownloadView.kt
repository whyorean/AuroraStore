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
import com.aurora.Constants
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.data.model.DownloadFile
import com.aurora.store.databinding.ViewDownloadBinding
import com.aurora.store.util.CommonUtil.getDownloadSpeedString
import com.aurora.store.util.CommonUtil.getETAString
import com.aurora.store.util.CommonUtil.humanReadableByteValue
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.tonyodev.fetch2.Status
import java.lang.reflect.Modifier
import java.util.Locale

@ModelView(
    autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT,
    baseModelClass = BaseView::class
)
class DownloadView : RelativeLayout {

    private lateinit var B: ViewDownloadBinding

    private val gson: Gson = GsonBuilder()
        .excludeFieldsWithModifiers(Modifier.STATIC, Modifier.TRANSIENT)
        .create()

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
        val view = inflate(context, R.layout.view_download, this)
        B = ViewDownloadBinding.bind(view)
    }

    @ModelProp
    fun download(downloadFile: DownloadFile) {
        val download = downloadFile.download
        val extras = download.extras.getString(Constants.STRING_EXTRA, "{}")
        val app = gson.fromJson(extras, App::class.java)

        app?.let {
            B.imgDownload.load(app.iconArtwork.url) {
                placeholder(R.drawable.bg_placeholder)
                transformations(RoundedCornersTransformation(32F))
            }
            B.txtTitle.text = app.displayName
        }

        B.txtStatus.text = download.status.name
            .lowercase(Locale.getDefault())
            .capitalize(Locale.getDefault())

        B.txtSize.text = StringBuilder()
            .append(humanReadableByteValue(download.downloaded, true))
            .append("/")
            .append(humanReadableByteValue(download.total, true))

        val file = download.file
        B.txtFile.text = file.substring(file.lastIndexOf("/") + 1)

        var progress = download.progress
        if (progress == -1) {
            progress = 0
        }

        B.progressDownload.progress = progress
        B.txtProgress.text = ("$progress%")

        B.txtEta.text = getETAString(context, download.etaInMilliSeconds)
        B.txtSpeed.text = getDownloadSpeedString(
            context,
            download.downloadedBytesPerSecond
        )

        when (download.status) {
            Status.DOWNLOADING, Status.QUEUED, Status.ADDED -> {
                B.txtSpeed.visibility = VISIBLE
                B.txtEta.visibility = VISIBLE
            }
            else -> {
                B.txtSpeed.visibility = INVISIBLE
                B.txtEta.visibility = INVISIBLE
            }
        }
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
