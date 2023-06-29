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
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.File
import java.util.UUID

fun Context.getInternalBaseDirectory(): String {
    return (getExternalFilesDir(null) ?: filesDir).path
}

object PathUtil {

    private fun getDownloadDirectory(context: Context): String {
        return if (context.isExternalStorageEnable()) {
            getExternalPath()
        } else {
            context.getInternalBaseDirectory()
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
        val auroraDir =
            java.io.File("${Environment.getExternalStorageDirectory().absolutePath}/Aurora/Store")
        auroraDir.mkdirs()
        return auroraDir.absolutePath
    }

    fun getBaseCopyDirectory(): String {
        return "${getExternalPath()}/Exports/"
    }

    private fun getObbDownloadPath(app: App): String {
        return Environment.getExternalStorageDirectory()
            .toString() + "/Android/obb/" + app.packageName
    }

    fun getObbDownloadFile(app: App, file: File): String {
        val obbDir = getObbDownloadPath(app)
        return "$obbDir/${file.name}"
    }

    fun needsStorageManagerPerm(fileList: List<File>): Boolean {
        return fileList.any { it.type == File.FileType.OBB || it.type == File.FileType.PATCH }
    }

    fun getSpoofDirectory(context: Context): String {
        return "${context.getInternalBaseDirectory()}/SpoofConfigs"
    }

    fun getNewEmptySpoofConfig(context: Context): java.io.File {
        val file = java.io.File("${getSpoofDirectory(context)}/${UUID.randomUUID()}.properties")
        file.parentFile?.mkdirs()
        file.createNewFile()
        return file
    }
}

fun Context.isExternalStorageEnable(): Boolean {
    return Preferences.getBoolean(this, Preferences.PREFERENCE_DOWNLOAD_EXTERNAL)
}
