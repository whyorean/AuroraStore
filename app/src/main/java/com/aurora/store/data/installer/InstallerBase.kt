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

package com.aurora.store.data.installer

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.aurora.store.AuroraApp
import com.aurora.store.BuildConfig
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.data.room.download.Download
import com.aurora.store.util.PathUtil
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_AUTO_DELETE
import java.io.File

abstract class InstallerBase(protected var context: Context) : IInstaller {

    private val TAG = InstallerBase::class.java.simpleName

    var download: Download? = null
        private set

    override fun install(download: Download) {
        this.download = download
    }

    override fun clearQueue() {
        AuroraApp.enqueuedInstalls.clear()
    }

    override fun isAlreadyQueued(packageName: String): Boolean {
        return AuroraApp.enqueuedInstalls.contains(packageName)
    }

    override fun removeFromInstallQueue(packageName: String) {
        AuroraApp.enqueuedInstalls.remove(packageName)
    }

    fun onInstallationSuccess() {
        download?.let {
            AppInstaller.notifyInstallation(context, it.displayName, it.packageName)
            if (Preferences.getBoolean(context, PREFERENCE_AUTO_DELETE)) {
                PathUtil.getAppDownloadDir(context, it.packageName, it.versionCode)
                    .deleteRecursively()
            }
        }
    }

    open fun postError(packageName: String, error: String?, extra: String?) {
        Log.e(TAG, "Service Error :$error")

        val event = InstallerEvent.Failed(packageName).apply {
            this.error = error ?: ""
            this.extra = extra ?: ""
        }

        AuroraApp.events.send(event)
    }

    fun getFiles(
        packageName: String,
        versionCode: Int,
        sharedLibPackageName: String = ""
    ): List<File> {
        val downloadDir = if (sharedLibPackageName.isNotBlank()) {
            PathUtil.getLibDownloadDir(context, packageName, versionCode, sharedLibPackageName)
        } else {
            PathUtil.getAppDownloadDir(context, packageName, versionCode)
        }
        return downloadDir.listFiles()!!.filter { it.path.endsWith(".apk") }
    }

    fun getUri(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.fileProvider",
            file
        )
    }
}
