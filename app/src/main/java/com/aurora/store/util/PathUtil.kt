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
import com.aurora.extensions.isRAndAbove
import java.io.File
import java.util.UUID
import com.aurora.gplayapi.data.models.File as GPlayFile

fun Context.getInternalBaseDirectory(): String {
    return (getExternalFilesDir(null) ?: filesDir).path
}

object PathUtil {

    private const val libraries = "libraries"

    private fun getDownloadDirectory(context: Context): String {
        return if (context.isExternalStorageEnable()) {
            getExternalPath(context)
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
        versionCode: Int,
        sharedLibPackageName: String? = null
    ): String {
        return if (!sharedLibPackageName.isNullOrBlank()) {
            getLibDownloadDir(context, packageName, versionCode, sharedLibPackageName).absolutePath
        } else {
            getPackageDirectory(context, packageName) + "/$versionCode"
        }
    }

    fun getAppDownloadDir(context: Context, packageName: String, versionCode: Int): File {
        return File(getPackageDirectory(context, packageName), versionCode.toString())
    }

    fun getLibDownloadDir(
        context: Context,
        packageName: String,
        versionCode: Int,
        sharedLibPackageName: String
    ): File {
        return File(
            getAppDownloadDir(context, packageName, versionCode).absolutePath,
            "$libraries/$sharedLibPackageName"
        )
    }

    fun getApkDownloadFile(
        context: Context,
        packageName: String,
        versionCode: Int,
        file: GPlayFile,
        sharedLibPackageName: String? = null
    ): String {
        return getVersionDirectory(
            context,
            packageName,
            versionCode,
            sharedLibPackageName
        ) + "/${file.name}"
    }

    fun getZipFile(
        context: Context,
        packageName: String,
        versionCode: Int
    ): File {
        return File(
            getAppDownloadDir(
                context,
                packageName,
                versionCode,
            ), "${packageName}_${versionCode}.apks"
        )
    }

    fun getExternalPath(context: Context): String {
        val defaultDir =
            File("${Environment.getExternalStorageDirectory().absolutePath}/Aurora/Store")

        if (!defaultDir.exists())
            defaultDir.mkdirs()

        return Preferences.getString(
            context,
            Preferences.PREFERENCE_DOWNLOAD_DIRECTORY,
            defaultDir.absolutePath
        )
    }

    private fun getObbDownloadPath(packageName: String): String {
        return Environment.getExternalStorageDirectory()
            .toString() + "/Android/obb/" + packageName
    }

    fun getObbDownloadDir(packageName: String): File {
        return File(
            Environment.getExternalStorageDirectory().absolutePath,
            "/Android/obb/$packageName"
        )
    }

    fun getObbDownloadFile(packageName: String, file: GPlayFile): String {
        val obbDir = getObbDownloadPath(packageName)
        return "$obbDir/${file.name}"
    }

    fun needsStorageManagerPerm(fileList: List<GPlayFile>): Boolean {
        return fileList.any { it.type == GPlayFile.FileType.OBB || it.type == GPlayFile.FileType.PATCH }
    }

    fun getSpoofDirectory(context: Context): String {
        return "${context.getInternalBaseDirectory()}/SpoofConfigs"
    }

    fun getNewEmptySpoofConfig(context: Context): File {
        val file = File("${getSpoofDirectory(context)}/${UUID.randomUUID()}.properties")
        file.parentFile?.mkdirs()
        file.createNewFile()
        return file
    }

    fun canWriteToDirectory(context: Context, directoryPath: String): Boolean {
        val directory = if (directoryPath.startsWith("/")) {
            File(directoryPath)
        } else {
            File(context.getExternalFilesDir(null), directoryPath)
        }

        return if (isRAndAbove()) {
            Environment.isExternalStorageManager() && directory.exists() && directory.canWrite()
        } else {
            isExternalStorageAccessible(context) && directory.exists() && directory.canWrite()
        }
    }
}

fun Context.isExternalStorageEnable(): Boolean {
    return Preferences.getBoolean(this, Preferences.PREFERENCE_DOWNLOAD_EXTERNAL)
}
