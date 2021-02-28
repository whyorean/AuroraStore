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

package com.aurora.store.data.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.util.ArrayMap
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.aurora.Constants
import com.aurora.extensions.getStyledAttributeColor
import com.aurora.extensions.isLAndAbove
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.data.downloader.DownloadManager
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.data.receiver.DownloadCancelReceiver
import com.aurora.store.data.receiver.DownloadPauseReceiver
import com.aurora.store.data.receiver.DownloadResumeReceiver
import com.aurora.store.data.receiver.InstallReceiver
import com.aurora.store.util.CommonUtil
import com.aurora.store.util.Log
import com.aurora.store.view.ui.details.AppDetailsActivity
import com.aurora.store.view.ui.downloads.DownloadActivity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.tonyodev.fetch2.*
import org.apache.commons.lang3.StringUtils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.lang.reflect.Modifier
import java.util.*

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

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()

        Log.i("Notification Service Started")

        EventBus.getDefault().register(this)

        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        //Create Notification Channels : General & Alert
        createNotificationChannel()

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
        }

        fetch.addListener(fetchListener)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = ArrayList<NotificationChannel>()
            channels.add(
                NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_ALERT,
                    getString(R.string.notification_channel_alert),
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
            channels.add(
                NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_GENERAL,
                    getString(R.string.notification_channel_general),
                    NotificationManager.IMPORTANCE_MIN
                )
            )
            notificationManager.createNotificationChannels(channels)
        }
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
            Status.FAILED -> {
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
                val contentString = getString(R.string.download_progress)
                val partString = StringUtils.joinWith(
                    "/",
                    fetchGroup.completedDownloads.size + 1,
                    fetchGroup.downloads.size
                )
                val speedString: String =
                    CommonUtil.humanReadableByteSpeed(download.downloadedBytesPerSecond, true)
                progressBigText.bigText(
                    StringUtils.joinWith(
                        " \u2022 ",
                        contentString,
                        partString,
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
                val pauseString = getString(R.string.download_paused)
                val filesString = StringUtils.joinWith(
                    "/",
                    fetchGroup.completedDownloads.size,
                    fetchGroup.downloads.size
                )
                progressBigText.bigText(
                    StringUtils.joinWith(
                        " \u2022 ",
                        pauseString,
                        filesString
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

        if (isLAndAbove()) {
            when (status) {
                Status.DOWNLOADING -> builder.setCategory(Notification.CATEGORY_PROGRESS)
                Status.FAILED, Status.CANCELLED -> builder.setCategory(Notification.CATEGORY_ERROR)
                else -> builder.setCategory(Notification.CATEGORY_STATUS)
            }
        }

        notificationManager.notify(
            app.packageName,
            app.id,
            builder.build()
        )
    }

    private fun getPauseIntent(groupId: Int): PendingIntent {
        val intent = Intent(this, DownloadPauseReceiver::class.java)
        intent.putExtra(Constants.FETCH_GROUP_ID, groupId)
        return PendingIntent.getBroadcast(this, groupId, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun getResumeIntent(groupId: Int): PendingIntent {
        val intent = Intent(this, DownloadResumeReceiver::class.java)
        intent.putExtra(Constants.FETCH_GROUP_ID, groupId)
        return PendingIntent.getBroadcast(this, groupId, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun getCancelIntent(groupId: Int): PendingIntent {
        val intent = Intent(this, DownloadCancelReceiver::class.java)
        intent.putExtra(Constants.FETCH_GROUP_ID, groupId)
        return PendingIntent.getBroadcast(this, groupId, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun getContentIntentForDetails(app: App?): PendingIntent {
        val intent = Intent(this, AppDetailsActivity::class.java)
        intent.putExtra(Constants.STRING_EXTRA, gson.toJson(app))
        return PendingIntent.getActivity(
            this,
            packageName.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun getContentIntentForDownloads(): PendingIntent {
        val intent = Intent(this, DownloadActivity::class.java)
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun getInstallIntent(packageName: String, versionCode: String): PendingIntent {
        val intent = Intent(this, InstallReceiver::class.java)
        intent.putExtra(Constants.STRING_EXTRA, packageName)
        return PendingIntent.getBroadcast(
            this,
            packageName.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    @Subscribe()
    fun onEventMainThread(event: Any) {
        when (event) {
            is InstallerEvent.Success -> {
                val app = appMap[event.packageName.hashCode()]
                if (app != null)
                    notifyInstallationStatus(app, event.extra)
            }
            is InstallerEvent.Failed -> {
                val app = appMap[event.packageName.hashCode()]
                if (app != null)
                    notifyInstallationStatus(app, event.error)
            }
            else -> {

            }
        }
    }

    @Synchronized
    private fun install(packageName: String, files: List<Download>) {
        AppInstaller(this)
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
        builder.color = getStyledAttributeColor(R.attr.colorAccent)
        builder.setSmallIcon(R.drawable.ic_install)
        builder.setContentTitle(app.displayName)
        builder.setContentText(status)
        builder.setSubText(app.packageName)

        notificationManager.notify(
            app.packageName,
            app.id,
            builder.build()
        )
    }

    override fun onDestroy() {
        Log.i("Notification Service Stopped")
        fetch.removeListener(fetchListener)
        EventBus.getDefault().unregister(this);
        super.onDestroy()
    }
}