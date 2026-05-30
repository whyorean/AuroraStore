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
import androidx.core.content.pm.PackageInfoCompat
import com.aurora.gplayapi.data.models.PlayFile
import com.aurora.store.data.room.download.Download
import java.io.File
import java.util.UUID

object PathUtil {

    private const val LIBRARIES = "libraries"
    private const val DOWNLOADS = "Downloads"
    private const val SPOOF = "SpoofConfigs"

    fun getOldDownloadDirectories(context: Context): List<File> = listOf(
        File(context.filesDir, DOWNLOADS), // till 4.4.2
        File(context.getExternalFilesDir(null), DOWNLOADS) // till 4.4.2
    )

    fun getDownloadDirectory(context: Context): File = File(context.cacheDir, DOWNLOADS)

    private fun getPackageDirectory(context: Context, packageName: String): File =
        File(getDownloadDirectory(context), packageName)

    fun getAppDownloadDir(context: Context, packageName: String, versionCode: Long): File =
        File(getPackageDirectory(context, packageName), versionCode.toString())

    fun getLibDownloadDir(
        context: Context,
        packageName: String,
        versionCode: Long,
        sharedLibPackageName: String
    ): File = File(
        getAppDownloadDir(context, packageName, versionCode),
        "$LIBRARIES/$sharedLibPackageName"
    )

    /**
     * Returns an instance of java's [File] class for the given [PlayFile]
     * @param context [Context]
     * @param playFile [PlayFile] to download
     * @param download An instance of [Download]
     */
    fun getLocalFile(context: Context, playFile: PlayFile, download: Download): File {
        val sharedLib = download.sharedLibs.find { it.fileList.contains(playFile) }
        return when (playFile.type) {
            PlayFile.Type.BASE, PlayFile.Type.SPLIT -> {
                val downloadDir = if (sharedLib != null) {
                    getLibDownloadDir(
                        context,
                        download.packageName,
                        download.versionCode,
                        sharedLib.packageName
                    )
                } else {
                    getAppDownloadDir(context, download.packageName, download.versionCode)
                }
                return File(downloadDir, playFile.name)
            }

            PlayFile.Type.OBB, PlayFile.Type.PATCH -> {
                File(getObbDownloadDir(download.packageName), playFile.name)
            }
        }
    }

    /**
     * Resolves every on-disk file belonging to a finished [Download] (base/split APKs,
     * shared-library APKs and OBB/patch files), mapped to the relative path it should
     * occupy inside an exported zip bundle.
     *
     * APKs are taken from the download cache, falling back to the APKs of the installed
     * app when the cache was cleared (e.g. auto-deleted after install). They keep their
     * layout relative to the app's download directory (shared libraries stay under
     * `libraries/<packageName>/`). OBB/patch files live outside the cache, survive the
     * cleanup, and are placed under `Android/obb/<packageName>/` so they can be restored
     * to their on-device location.
     *
     * Files that are missing on disk are skipped, as OBB/patch files are optional.
     */
    fun getExportableFiles(context: Context, download: Download): Map<String, File> {
        val appDir = getAppDownloadDir(context, download.packageName, download.versionCode)
        val playFiles = download.fileList + download.sharedLibs.flatMap { it.fileList }
        val (obbPlayFiles, apkPlayFiles) = playFiles.partition {
            it.type == PlayFile.Type.OBB || it.type == PlayFile.Type.PATCH
        }

        val apkFiles = apkPlayFiles.associate { playFile ->
            val file = getLocalFile(context, playFile, download)
            file.relativeTo(appDir).invariantSeparatorsPath to file
        }.filterValues { it.exists() }
            .ifEmpty { getInstalledApkFiles(context, download.packageName, download.versionCode) }

        val obbFiles = obbPlayFiles.associate { playFile ->
            val file = getLocalFile(context, playFile, download)
            "Android/obb/${download.packageName}/${file.name}" to file
        }.filterValues { it.exists() }

        return apkFiles + obbFiles
    }

    /**
     * Resolves the base and split APKs of the installed [packageName] from their on-device
     * locations, used as a fallback when the downloaded files are no longer cached. Returns
     * an empty map unless the installed version matches [versionCode], so a different
     * installed version is never exported by mistake.
     */
    private fun getInstalledApkFiles(
        context: Context,
        packageName: String,
        versionCode: Long
    ): Map<String, File> {
        val packageInfo = runCatching {
            PackageUtil.getPackageInfo(context, packageName)
        }.getOrNull() ?: return emptyMap()

        if (PackageInfoCompat.getLongVersionCode(packageInfo) != versionCode) return emptyMap()

        val appInfo = packageInfo.applicationInfo ?: return emptyMap()
        val apkPaths = buildList {
            appInfo.sourceDir?.let { add(it) }
            appInfo.splitSourceDirs?.let { addAll(it.filterNotNull()) }
        }

        return apkPaths.map { File(it) }
            .filter { it.exists() }
            .associateBy { it.name }
    }

    fun getZipFile(context: Context, packageName: String, versionCode: Long): File = File(
        getAppDownloadDir(
            context,
            packageName,
            versionCode
        ),
        "${packageName}_$versionCode.apks"
    )

    fun getObbDownloadDir(packageName: String): File =
        File(Environment.getExternalStorageDirectory(), "/Android/obb/$packageName")

    fun getSpoofDirectory(context: Context): File = File(context.filesDir, SPOOF)

    fun getNewEmptySpoofConfig(context: Context): File {
        val file = File(getSpoofDirectory(context), "${UUID.randomUUID()}.properties")
        file.parentFile?.mkdirs()
        file.createNewFile()
        return file
    }
}
