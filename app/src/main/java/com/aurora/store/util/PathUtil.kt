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
import java.io.File
import java.util.UUID
import com.aurora.gplayapi.data.models.File as GPlayFile

object PathUtil {

    private const val LIBRARIES = "libraries"
    private const val DOWNLOADS = "Downloads"
    private const val SPOOF = "SpoofConfigs"

    fun getOldDownloadDirectories(context: Context): List<File> {
        return listOf(
            File(context.filesDir, DOWNLOADS), // till 4.4.2
            File(context.getExternalFilesDir(null), DOWNLOADS) // till 4.4.2
        )
    }

    fun getDownloadDirectory(context: Context): File {
        return File(context.cacheDir, DOWNLOADS)
    }

    private fun getPackageDirectory(context: Context, packageName: String): File {
        return File(getDownloadDirectory(context), packageName)
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
            getAppDownloadDir(context, packageName, versionCode),
            "$LIBRARIES/$sharedLibPackageName"
        )
    }

    fun getApkDownloadFile(
        context: Context,
        packageName: String,
        versionCode: Int,
        file: GPlayFile,
        sharedLibPackageName: String? = null
    ): File {
        val downloadDir = if (!sharedLibPackageName.isNullOrBlank()) {
            getLibDownloadDir(context, packageName, versionCode, sharedLibPackageName)
        } else {
            File(getPackageDirectory(context, packageName), versionCode.toString())
        }
        return File(downloadDir, file.name)
    }

    fun getZipFile(context: Context, packageName: String, versionCode: Int): File {
        return File(
            getAppDownloadDir(
                context,
                packageName,
                versionCode,
            ), "${packageName}_${versionCode}.apks"
        )
    }

    fun getObbDownloadDir(packageName: String): File {
        return File(Environment.getExternalStorageDirectory(), "/Android/obb/$packageName")
    }

    fun getObbDownloadFile(packageName: String, file: GPlayFile): File {
        return File(getObbDownloadDir(packageName), file.name)
    }

    fun needsStorageManagerPerm(fileList: List<GPlayFile>): Boolean {
        return fileList.any { it.type == GPlayFile.FileType.OBB || it.type == GPlayFile.FileType.PATCH }
    }

    fun getSpoofDirectory(context: Context): File {
        return File(context.filesDir, SPOOF)
    }

    fun getNewEmptySpoofConfig(context: Context): File {
        val file = File(getSpoofDirectory(context), "${UUID.randomUUID()}.properties")
        file.parentFile?.mkdirs()
        file.createNewFile()
        return file
    }

    fun canReadWriteOBB(): Boolean {
        return canReadWriteDir(
            Environment.getExternalStorageDirectory().toString() + "/Android/obb/"
        )
    }

    private fun canReadWriteDir(dir: String): Boolean {
        return File(dir).let { it.exists() && it.canRead() && it.canWrite() }
    }
}

