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
import android.net.Uri
import android.util.Log
import com.aurora.store.util.PackageUtil.getPackageInfo
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ApkCopier {

    private const val TAG = "ApkCopier"

    fun copy(context: Context, packageName: String, uri: Uri) {
        val packageInfo = getPackageInfo(context, packageName, PackageManager.GET_META_DATA)
        val baseApk = getBaseApk(packageInfo)
        val fileList: MutableList<File?> = mutableListOf()

        /*Add base APK*/
        fileList.add(baseApk)

        if (!packageInfo.applicationInfo.splitSourceDirs.isNullOrEmpty()) {
            fileList.addAll(getSplitAPKs(packageInfo))
        }
        bundleAllAPKs(context, fileList.filterNotNull(), uri)
    }

    private fun getBaseApk(packageInfo: PackageInfo?): File? {
        return if (packageInfo?.applicationInfo != null) {
            File(packageInfo.applicationInfo.sourceDir)
        } else {
            null
        }
    }

    private fun getSplitAPKs(packageInfo: PackageInfo): MutableList<File> {
        val fileList: MutableList<File> = ArrayList()
        val splitSourceDirs = packageInfo.applicationInfo.splitSourceDirs
        if (splitSourceDirs != null) {
            for (fileName in splitSourceDirs) fileList.add(File(fileName))
        }
        return fileList
    }

    private fun bundleAllAPKs(context: Context, fileList: List<File>, uri: Uri) {
        try {
            val zipOutputStream = ZipOutputStream(context.contentResolver.openOutputStream(uri))

            fileList.forEach {  file ->
                file.inputStream().use { output ->
                    val zipEntry = ZipEntry(file.name)
                    zipOutputStream.putNextEntry(zipEntry)
                    output.copyTo(zipOutputStream)
                    zipOutputStream.closeEntry()
                }
            }

            zipOutputStream.close()
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to copy app bundles", exception)
        }
    }
}
