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

package com.aurora.store.data.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.aurora.extensions.goAsync
import com.aurora.store.data.event.BusEvent.InstallEvent
import com.aurora.store.data.event.BusEvent.UninstallEvent
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.data.model.DownloadStatus
import com.aurora.store.data.room.download.Download
import com.aurora.store.util.DownloadWorkerUtil
import com.aurora.store.util.NotificationUtil
import com.aurora.store.util.PackageUtil.isSharedLibrary
import com.aurora.store.util.PathUtil
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_AUTO_DELETE
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import org.greenrobot.eventbus.EventBus

@AndroidEntryPoint
open class PackageManagerReceiver : BroadcastReceiver() {

    private val TAG = PackageManagerReceiver::class.java.simpleName

    @Inject
    lateinit var downloadWorkerUtil: DownloadWorkerUtil

    @Inject
    lateinit var appInstaller: AppInstaller

    override fun onReceive(context: Context, intent: Intent) = goAsync {
        if (intent.action != null && intent.data != null) {
            val packageName = intent.data!!.encodedSchemeSpecificPart

            // We don't care about shared libraries, bail out
            if (isSharedLibrary(context, packageName)) return@goAsync

            when (intent.action) {
                Intent.ACTION_PACKAGE_ADDED -> {
                    Log.i(TAG, "Installed $packageName")
                    EventBus.getDefault().post(InstallEvent(packageName, ""))

                    downloadWorkerUtil.downloadsList.filter { it.isNotEmpty() }.firstOrNull()
                        ?.find { it.packageName == packageName && it.downloadStatus == DownloadStatus.COMPLETED }
                        ?.let {
                            notifyInstallation(context, it)
                            if (Preferences.getBoolean(context, PREFERENCE_AUTO_DELETE)) {
                                clearDownloads(context, it)
                            }
                        }
                }

                Intent.ACTION_PACKAGE_REMOVED -> {
                    EventBus.getDefault().post(UninstallEvent(packageName, ""))
                }
            }

            //Clear installation queue
            appInstaller.getPreferredInstaller().removeFromInstallQueue(packageName)
        }
    }

    private fun clearDownloads(context: Context, download: Download) {
        try {
            PathUtil.getAppDownloadDir(context, download.packageName, download.versionCode)
                .deleteRecursively()
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to delete $download.packageName's downloads", exception)
        }
    }

    private fun notifyInstallation(context: Context, download: Download) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationUtil.getInstallNotification(context, download)
        notificationManager.notify(download.packageName.hashCode(), notification)
    }
}
