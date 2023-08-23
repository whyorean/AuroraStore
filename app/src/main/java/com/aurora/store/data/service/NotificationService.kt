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

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.IBinder
import android.util.ArrayMap
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.navigation.NavDeepLinkBuilder
import com.aurora.Constants
import com.aurora.extensions.getStyledAttributeColor
import com.aurora.extensions.isMAndAbove
import com.aurora.gplayapi.data.models.App
import com.aurora.store.MainActivity
import com.aurora.store.R
import com.aurora.store.data.downloader.DownloadManager
import com.aurora.store.data.downloader.RequestGroupIdBuilder
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.data.receiver.DownloadCancelReceiver
import com.aurora.store.data.receiver.DownloadPauseReceiver
import com.aurora.store.data.receiver.DownloadResumeReceiver
import com.aurora.store.data.receiver.InstallReceiver
import com.aurora.store.util.CommonUtil
import com.aurora.store.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.tonyodev.fetch2.AbstractFetchGroupListener
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.FetchGroup
import com.tonyodev.fetch2.Status
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.lang.reflect.Modifier

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
        val status = download.status

        //Ignore notifications for completion of sub-parts of a bundled apk
        if (status == Status.COMPLETED && fetchGroup.groupDownloadProgress < 100)
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

        if (status == Status.DELETED) {
            notificationManager.cancel(app.id)
            return
        }

        val builder = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_GENERAL)
        builder.setContentTitle(app.displayName)
        builder.setSmallIcon(R.drawable.ic_notification_outlined)
        builder.color = ContextCompat.getColor(this, R.color.colorAccent)
        builder.setWhen(download.created)
        builder.setContentIntent(getContentIntentForDownloads())

        when (status) {
            Status.PAUSED -> {
                builder.setSmallIcon(R.drawable.ic_download_pause)
                builder.setContentText(getString(R.string.download_paused))
            }
            Status.CANCELLED -> {
                builder.setSmallIcon(R.drawable.ic_download_cancel)
                builder.setContentText(getString(R.string.download_canceled))
                builder.color = Color.RED
            }
            Status.FAILED ->
            {
                builder.setSmallIcon(R.drawable.ic_download_fail)
                builder.setContentText(getString(R.string.download_failed))
                builder.color = Color.RED
            }
            Status.COMPLETED -> if (fetchGroup.groupDownloadProgress == 100) {
                builder.setSmallIcon(android.R.drawable.stat_sys_download_done)
                builder.setContentText(getString(R.string.download_completed))
            }
            else -> {
                builder.setSmallIcon(android.R.drawable.stat_sys_download)
                builder.setContentText(getString(R.string.download_metadata))
            }
        }
        val progress = fetchGroup.groupDownloadProgress
        val progressBigText = NotificationCompat.BigTextStyle()

        when (status) {
            Status.QUEUED -> {
                builder.setProgress(100, 0, true)
                progressBigText.bigText(getString(R.string.download_queued))
                builder.setStyle(progressBigText)
            }

            Status.DOWNLOADING -> {
                val speedString: String =
                    CommonUtil.humanReadableByteSpeed(download.downloadedBytesPerSecond, true)
                progressBigText.bigText(
                    getString(
                        R.string.download_progress,
                        fetchGroup.completedDownloads.size + 1,
                        fetchGroup.downloads.size,
                        speedString
                    )
                )
                builder.setStyle(progressBigText)
                builder.addAction(
                    NotificationCompat.Action.Builder(
                        R.drawable.ic_download_pause,
                        getString(R.string.action_pause),
                        getPauseIntent(groupId)
                    ).build()
                )
                builder.addAction(
                    NotificationCompat.Action.Builder(
                        R.drawable.ic_download_cancel,
                        getString(R.string.action_cancel),
                        getCancelIntent(groupId)
                    ).build()
                )
                if (progress < 0) builder.setProgress(
                    100,
                    0,
                    true
                ) else builder.setProgress(100, progress, false)
            }

            Status.PAUSED -> {
                progressBigText.bigText(
                    getString(
                        R.string.download_paused,
                        fetchGroup.completedDownloads.size,
                        fetchGroup.downloads.size
                    )
                )
                builder.setStyle(progressBigText)
                builder.addAction(
                    NotificationCompat.Action.Builder(
                        R.drawable.ic_download_pause,
                        getString(R.string.action_resume),
                        getResumeIntent(groupId)
                    ).build()
                )
            }

            Status.COMPLETED -> if (fetchGroup.groupDownloadProgress == 100) {
                builder.setAutoCancel(true)
                builder.setContentIntent(getContentIntentForDetails(app))
                builder.setStyle(progressBigText)
            }
            else -> {

            }
        }

        when (status) {
            Status.DOWNLOADING -> builder.setCategory(Notification.CATEGORY_PROGRESS)
            Status.FAILED, Status.CANCELLED -> builder.setCategory(Notification.CATEGORY_ERROR)
            else -> builder.setCategory(Notification.CATEGORY_STATUS)
        }

        if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            notificationManager.notify(
                app.packageName,
                app.id,
                builder.build()
            )
        }
    }

    private fun getPauseIntent(groupId: Int): PendingIntent {
        val intent = Intent(this, DownloadPauseReceiver::class.java)
        intent.putExtra(Constants.FETCH_GROUP_ID, groupId)
        val flags = if (isMAndAbove())
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_CANCEL_CURRENT
        return PendingIntent.getBroadcast(this, groupId, intent, flags)
    }

    private fun getResumeIntent(groupId: Int): PendingIntent {
        val intent = Intent(this, DownloadResumeReceiver::class.java)
        intent.putExtra(Constants.FETCH_GROUP_ID, groupId)
        val flags = if (isMAndAbove())
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_CANCEL_CURRENT
        return PendingIntent.getBroadcast(this, groupId, intent, flags)
    }

    private fun getCancelIntent(groupId: Int): PendingIntent {
        val intent = Intent(this, DownloadCancelReceiver::class.java)
        intent.putExtra(Constants.FETCH_GROUP_ID, groupId)
        val flags = if (isMAndAbove())
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_CANCEL_CURRENT
        return PendingIntent.getBroadcast(this, groupId, intent, flags)
    }

    private fun getContentIntentForDetails(app: App?): PendingIntent {
        return NavDeepLinkBuilder(this)
            .setGraph(R.navigation.mobile_navigation)
            .setDestination(R.id.appDetailsFragment)
            .setComponentName(MainActivity::class.java)
            .setArguments(bundleOf("packageName" to app!!.packageName))
            .createPendingIntent()
    }

    private fun getContentIntentForDownloads(): PendingIntent {
        return NavDeepLinkBuilder(this)
            .setGraph(R.navigation.mobile_navigation)
            .setDestination(R.id.downloadFragment)
            .setComponentName(MainActivity::class.java)
            .createPendingIntent()
    }

    private fun getInstallIntent(packageName: String): PendingIntent {
        val intent = Intent(this, InstallReceiver::class.java)
        intent.putExtra(Constants.STRING_EXTRA, packageName)
        val flags = if (isMAndAbove())
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_CANCEL_CURRENT
        return PendingIntent.getBroadcast(
            this,
            packageName.hashCode(),
            intent,
            flags
        )
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
        val builder = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ALERT)
        builder.color = getStyledAttributeColor(R.color.colorAccent)
        builder.setSmallIcon(R.drawable.ic_install)
        builder.setContentTitle(app.displayName)
        builder.setContentText(status)
        builder.setSubText(app.packageName)

        if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            notificationManager.notify(
                app.packageName,
                app.id,
                builder.build()
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
