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

package com.aurora.store.data.downloader

import android.content.Context
import com.aurora.Constants
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.File
import com.aurora.store.util.PathUtil
import com.aurora.store.util.Preferences
import com.google.gson.GsonBuilder
import com.tonyodev.fetch2.EnqueueAction
import com.tonyodev.fetch2.NetworkType
import com.tonyodev.fetch2.Request
import com.tonyodev.fetch2core.Extras
import java.lang.reflect.Modifier

private fun Request.attachMetaData(context: Context, app: App) {
    val isWifiOnly = Preferences.getBoolean(context, Preferences.PREFERENCE_DOWNLOAD_WIFI_ONLY)

    apply {
        groupId = app.getGroupId(context)
        tag = app.packageName
        enqueueAction = EnqueueAction.UPDATE_ACCORDINGLY
        networkType = if (isWifiOnly) NetworkType.WIFI_ONLY else NetworkType.ALL
    }
}

private fun Request.attachExtra(app: App) {
    val stringMap: MutableMap<String, String> = mutableMapOf()
    val gson = GsonBuilder()
        .excludeFieldsWithModifiers(Modifier.TRANSIENT)
        .create()
    stringMap[Constants.STRING_EXTRA] = gson.toJson(app)
    apply {
        extras = Extras(stringMap)
    }
}

object RequestBuilder {

    fun buildRequest(context: Context, app: App, file: File): Request {
        val fileName = when (file.type) {
            File.FileType.BASE,
            File.FileType.SPLIT -> PathUtil.getApkDownloadFile(context, app.packageName, app.versionCode, file)
            File.FileType.OBB,
            File.FileType.PATCH -> PathUtil.getObbDownloadFile(app.packageName, file)
        }
        return Request(file.url, fileName).apply {
            attachMetaData(context, app)
            attachExtra(app)
        }
    }
}
