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

package com.aurora.store.data.installer.base

import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageInstaller
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.content.getSystemService
import com.aurora.store.AuroraApp
import com.aurora.store.BuildConfig
import com.aurora.store.R
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.data.room.download.Download
import com.aurora.store.util.NotificationUtil
import com.aurora.store.util.PathUtil
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_AUTO_DELETE
import java.io.File

abstract class InstallerBase(private val context: Context) : IInstaller {

    companion object {
        fun notifyInstallation(context: Context, displayName: String, packageName: String) {
            val notificationManager = context.getSystemService<NotificationManager>()
            val notification = NotificationUtil.getInstallNotification(context, displayName, packageName)
            notificationManager!!.notify(packageName.hashCode(), notification)
        }

        fun getErrorString(context: Context, status: Int): String {
            return when (status) {
                PackageInstaller.STATUS_FAILURE_ABORTED -> context.getString(R.string.installer_status_user_action)
                PackageInstaller.STATUS_FAILURE_BLOCKED -> context.getString(R.string.installer_status_failure_blocked)
                PackageInstaller.STATUS_FAILURE_CONFLICT -> context.getString(R.string.installer_status_failure_conflict)
                PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> context.getString(R.string.installer_status_failure_incompatible)
                PackageInstaller.STATUS_FAILURE_INVALID -> context.getString(R.string.installer_status_failure_invalid)
                PackageInstaller.STATUS_FAILURE_STORAGE -> context.getString(R.string.installer_status_failure_storage)
                else -> context.getString(R.string.installer_status_failure)
            }
        }
    }

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
            notifyInstallation(context, it.displayName, it.packageName)
            if (Preferences.getBoolean(context, PREFERENCE_AUTO_DELETE)) {
                PathUtil.getAppDownloadDir(context, it.packageName, it.versionCode)
                    .deleteRecursively()
            }
        }
    }

    open fun postError(packageName: String, error: String?, extra: String?) {
        Log.e(TAG, "Installer Error :$error")
        AuroraApp.events.send(
            InstallerEvent.Failed(
                packageName = packageName,
                error = error,
                extra = extra
            )
        )
    }

    fun getFiles(
        packageName: String,
        versionCode: Long,
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
