/*
 * Aurora Store
 *  Copyright (C) 2021, Rahul Kumar Patel <whyorean@gmail.com>
 *  Copyright (C) 2022, The Calyx Institute
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

package com.aurora.store.data.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.ArrayMap
import androidx.core.app.NotificationManagerCompat
import com.aurora.Constants
import com.aurora.gplayapi.data.models.App
import com.aurora.store.data.downloader.DownloadManager
import com.aurora.store.data.downloader.RequestGroupIdBuilder
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.util.Log
import com.aurora.store.util.NotificationUtil
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.tonyodev.fetch2.AbstractFetchGroupListener
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.FetchGroup
import com.tonyodev.fetch2.Status
import java.lang.reflect.Modifier
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class NotificationService : Service() {

    companion object {
        fun startService(context: Context) {
            try {
                context.startService(Intent(context, NotificationService::class.java))
            } catch (e: Exception) {
                Log.e("Failed to start notification service : %s", e.message)
            }
        }
    }

    private lateinit var fetch: Fetch
    private lateinit var fetchListener: AbstractFetchGroupListener
    private lateinit var notificationManager: NotificationManager

    private val appMap = ArrayMap<Int, App>()
    private val gson: Gson = GsonBuilder()
        .excludeFieldsWithModifiers(Modifier.STATIC, Modifier.TRANSIENT)
        .create()

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        Log.i("Notification Service Started")

        EventBus.getDefault().register(this)

        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        fetch = DownloadManager.with(this).fetch

        fetchListener = object : AbstractFetchGroupListener() {
            override fun onCancelled(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                showNotification(groupId, download, fetchGroup)
            }

            override fun onCompleted(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                showNotification(groupId, download, fetchGroup)
            }

            override fun onError(
                groupId: Int,
                download: Download,
                error: Error,
                throwable: Throwable?,
                fetchGroup: FetchGroup
            ) {
                showNotification(groupId, download, fetchGroup)
            }

            override fun onProgress(
                groupId: Int,
                download: Download,
                etaInMilliSeconds: Long,
                downloadedBytesPerSecond: Long,
                fetchGroup: FetchGroup
            ) {
                showNotification(groupId, download, fetchGroup)
            }

            override fun onQueued(
                groupId: Int,
                download: Download,
                waitingNetwork: Boolean,
                fetchGroup: FetchGroup
            ) {
                showNotification(groupId, download, fetchGroup)
            }

            override fun onPaused(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                showNotification(groupId, download, fetchGroup)
            }

            override fun onDeleted(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                showNotification(groupId, download, fetchGroup)
            }
        }

        fetch.addListener(fetchListener)
    }

    private fun showNotification(groupId: Int, download: Download, fetchGroup: FetchGroup) {
        //Ignore notifications for completion of sub-parts of a bundled apk
        if (download.status == Status.COMPLETED && fetchGroup.groupDownloadProgress < 100)
            return

        //synchronized(appMap) {
        var app: App? = appMap[groupId]

        if (app == null) {
            app = gson.fromJson(
                download.extras.getString(Constants.STRING_EXTRA, "{}"),
                App::class.java
            )
            appMap[groupId] = app
        }

        if (app == null)
            return

        if (download.status == Status.DELETED) {
            notificationManager.cancel(app.id)
            return
        }

        if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            notificationManager.notify(
                app.packageName,
                app.id,
                NotificationUtil.getDownloadNotification(this, app, groupId, download, fetchGroup)
            )
        }
    }

    @Subscribe()
    fun onEventMainThread(event: Any) {
        when (event) {
            is InstallerEvent.Success -> {
                val groupIDsOfPackageName = RequestGroupIdBuilder.getGroupIDsForApp(this, event.packageName.hashCode())
                var app: App? = null
                for (item in groupIDsOfPackageName) {
                    app = appMap[item]
                    if (app != null) {
                        break
                    }
                }
                if (app != null)
                    notifyInstallationStatus(app, event.extra)
            }
            is InstallerEvent.Failed -> {
                val groupIDsOfPackageName = RequestGroupIdBuilder.getGroupIDsForApp(this, event.packageName.hashCode())
                var app: App? = null
                for (item in groupIDsOfPackageName) {
                    app = appMap[item]
                    if (app != null) {
                        break
                    }
                }
                if (app != null)
                    notifyInstallationStatus(app, event.error)
            }
            else -> {

            }
        }
    }

    @Synchronized
    private fun install(packageName: String, files: List<Download>) {
        AppInstaller.getInstance(this)
            .getPreferredInstaller()
            .install(
                packageName,
                files
                    .filter { it.file.endsWith(".apk") }
                    .map {
                        it.file
                    }.toList()
            )
    }

    private fun notifyInstallationStatus(app: App, status: String?) {
        if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            notificationManager.notify(
                app.packageName,
                app.id,
                NotificationUtil.getInstallNotification(this, app, status)
            )
        }
    }

    override fun onDestroy() {
        Log.i("Notification Service Stopped")
        fetch.removeListener(fetchListener)
        EventBus.getDefault().unregister(this);
        super.onDestroy()
    }
}
