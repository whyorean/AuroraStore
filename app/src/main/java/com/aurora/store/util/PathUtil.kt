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

package com.aurora.store.util

import android.content.Context
import android.os.Environment
import com.aurora.extensions.isLAndAbove
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.File

fun Context.getInternalBaseDirectory(): String {
    return filesDir.path
}

object PathUtil {

    private fun getDownloadDirectory(context: Context): String {
        return if (isLAndAbove()) {
            if (context.isExternalStorageEnable())
                getExternalPath()
            else
                context.getInternalBaseDirectory() + "/Downloads"
        } else {
            getExternalPath()
        }
    }

    fun getPackageDirectory(context: Context, packageName: String): String {
        return getDownloadDirectory(context) + "/Downloads/$packageName"
    }

    private fun getVersionDirectory(
        context: Context,
        packageName: String,
        versionCode: Int
    ): String {
        return getPackageDirectory(context, packageName) + "/$versionCode"
    }

    fun getApkDownloadFile(context: Context, app: App, file: File): String {
        return getVersionDirectory(context, app.packageName, app.versionCode) + "/${file.name}"
    }

    fun getApkDownloadFile(context: Context, packageName: String, versionCode: Int): String {
        return getVersionDirectory(context, packageName, versionCode)
    }

    fun getExternalPath(): String {
        return Environment.getExternalStorageDirectory().toString() + "/Aurora/Store"
    }

    fun getBaseCopyDirectory(): String {
        return "${getExternalPath()}/Exports/"
    }

    private fun getObbDownloadPath(context: Context, app: App): String {
        return Environment.getExternalStorageDirectory()
            .toString() + "/Android/obb/" + app.packageName
    }

    fun getObbDownloadFile(context: Context, app: App, file: File): String {
        val obbDir = getObbDownloadPath(context, app)
        return "$obbDir/${file.name}"
    }
}

fun Context.isExternalStorageEnable(): Boolean {
    return Preferences.getBoolean(this, Preferences.PREFERENCE_DOWNLOAD_EXTERNAL)
}