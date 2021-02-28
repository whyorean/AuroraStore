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
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.aurora.extensions.isLAndAbove
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ApkCopier(private val context: Context, private val packageName: String) {

    fun copy() {
        val destination = File(PathUtil.getBaseCopyDirectory())

        destination.let {
            if (it.exists()) {
                Log.i("Base copy directory is available")
            } else {
                it.mkdirs()
                Log.e("Base copy directory is created : ${it.path}")
            }
        }

        val packageInfo: PackageInfo = context.packageManager.getPackageInfo(
            packageName,
            PackageManager.GET_META_DATA
        )

        val baseApk = getBaseApk(packageInfo)
        val fileList: MutableList<File?> = mutableListOf()

        /*Add base APK*/
        fileList.add(baseApk)

        if (isLAndAbove()) {
            val splitSourceDirs = packageInfo.applicationInfo.splitSourceDirs
            if (splitSourceDirs != null && splitSourceDirs.isNotEmpty()) {
                /*Add Split APKs*/
                fileList.addAll(getSplitAPKs(packageInfo))
            }
            bundleAllAPKs(fileList)
        } else {
            bundleAllAPKs(fileList)
        }
    }

    private fun getBaseApk(packageInfo: PackageInfo?): File? {
        return if (packageInfo?.applicationInfo != null) {
            File(packageInfo.applicationInfo.sourceDir)
        } else {
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun getSplitAPKs(packageInfo: PackageInfo): MutableList<File> {
        val fileList: MutableList<File> = ArrayList()
        val splitSourceDirs = packageInfo.applicationInfo.splitSourceDirs
        if (splitSourceDirs != null) {
            for (fileName in splitSourceDirs) fileList.add(File(fileName))
        }
        return fileList
    }

    private fun bundleAllAPKs(fileList: List<File?>) {
        try {
            val fileOutputStream =
                FileOutputStream(PathUtil.getBaseCopyDirectory() + "$packageName.zip")
            val zipOutputStream = ZipOutputStream(fileOutputStream)

            for (file in fileList) {
                file?.let {
                    val zipEntry = ZipEntry(file.name)
                    zipOutputStream.putNextEntry(zipEntry)
                    IOUtils.copy(it.inputStream(), zipOutputStream)
                    zipOutputStream.closeEntry()
                }
            }

            zipOutputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("ApkCopier : %s", e.message)
        }
    }
}