package com.aurora.store.util

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.navigation.NavDeepLinkBuilder
import androidx.work.WorkManager
import com.aurora.Constants
import com.aurora.extensions.getStyledAttributeColor
import com.aurora.extensions.isMAndAbove
import com.aurora.gplayapi.data.models.App
import com.aurora.store.MainActivity
import com.aurora.store.R
import com.aurora.store.data.activity.InstallActivity
import com.aurora.store.data.model.DownloadStatus
import com.aurora.store.data.receiver.DownloadCancelReceiver
import com.aurora.store.data.receiver.DownloadPauseReceiver
import com.aurora.store.data.receiver.DownloadResumeReceiver
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.FetchGroup
import com.tonyodev.fetch2.Status
import java.util.UUID

object NotificationUtil {

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Application.NOTIFICATION_SERVICE) as NotificationManager
            val channels = ArrayList<NotificationChannel>()
            channels.add(
                NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_ALERT,
                    context.getString(R.string.notification_channel_alert),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    setSound(null, null)
                }
            )
            channels.add(
                NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_GENERAL,
                    context.getString(R.string.notification_channel_general),
                    NotificationManager.IMPORTANCE_MIN
                )
            )
            channels.add(
                NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_UPDATER_SERVICE,
                    context.getString(R.string.notification_channel_updater_service),
                    NotificationManager.IMPORTANCE_MIN
                )
            )
            channels.add(
                NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_UPDATES,
                    context.getString(R.string.notification_channel_updates),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    setSound(null, null)
                }
            )
            notificationManager.createNotificationChannels(channels)
        }
    }

    fun getDownloadNotification(
        context: Context,
        app: App,
        groupId: Int,
        download: Download,
        fetchGroup: FetchGroup
    ): Notification {
        val status = download.status

        val builder = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_GENERAL)
        builder.setContentTitle(app.displayName)
        builder.setSmallIcon(R.drawable.ic_notification_outlined)
        builder.color = ContextCompat.getColor(context, R.color.colorAccent)
        builder.setWhen(download.created)
        builder.setContentIntent(getContentIntentForDownloads(context))

        when (status) {
            Status.PAUSED -> {
                builder.setSmallIcon(R.drawable.ic_download_pause)
                builder.setContentText(context.getString(R.string.download_paused))
            }

            Status.CANCELLED -> {
                builder.setSmallIcon(R.drawable.ic_download_cancel)
                builder.setContentText(context.getString(R.string.download_canceled))
                builder.color = Color.RED
            }

            Status.FAILED -> {
                builder.setSmallIcon(R.drawable.ic_download_fail)
                builder.setContentText(context.getString(R.string.download_failed))
                builder.color = Color.RED
            }

            Status.COMPLETED -> if (fetchGroup.groupDownloadProgress == 100) {
                builder.setSmallIcon(android.R.drawable.stat_sys_download_done)
                builder.setContentText(context.getString(R.string.download_completed))
            }

            else -> {
                builder.setSmallIcon(android.R.drawable.stat_sys_download)
                builder.setContentText(context.getString(R.string.download_metadata))
            }
        }
        val progress = fetchGroup.groupDownloadProgress
        val progressBigText = NotificationCompat.BigTextStyle()

        when (status) {
            Status.QUEUED -> {
                builder.setProgress(100, 0, true)
                progressBigText.bigText(context.getString(R.string.download_queued))
                builder.setStyle(progressBigText)
            }

            Status.DOWNLOADING -> {
                val speedString: String =
                    CommonUtil.humanReadableByteSpeed(download.downloadedBytesPerSecond, true)
                progressBigText.bigText(
                    context.getString(
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
                        context.getString(R.string.action_pause),
                        getPauseIntent(context, groupId)
                    ).build()
                )
                builder.addAction(
                    NotificationCompat.Action.Builder(
                        R.drawable.ic_download_cancel,
                        context.getString(R.string.action_cancel),
                        getCancelIntent(context, groupId)
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
                    context.getString(
                        R.string.download_paused,
                        fetchGroup.completedDownloads.size,
                        fetchGroup.downloads.size
                    )
                )
                builder.setStyle(progressBigText)
                builder.addAction(
                    NotificationCompat.Action.Builder(
                        R.drawable.ic_download_pause,
                        context.getString(R.string.action_resume),
                        getResumeIntent(context, groupId)
                    ).build()
                )
            }

            Status.COMPLETED -> if (fetchGroup.groupDownloadProgress == 100) {
                builder.setAutoCancel(true)
                builder.setContentIntent(getContentIntentForDetails(context, app))
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

        return builder.build()
    }

    fun getDownloadNotification(
        context: Context,
        app: App,
        status: DownloadStatus,
        progress: Int,
        workID: UUID
    ): Notification {
        val builder = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_GENERAL)
        builder.setContentTitle(app.displayName)
        builder.color = ContextCompat.getColor(context, R.color.colorAccent)
        builder.setContentIntent(getContentIntentForDownloads(context))

        when (status) {
            DownloadStatus.CANCELLED -> {
                builder.setSmallIcon(R.drawable.ic_download_cancel)
                builder.setContentText(context.getString(R.string.download_canceled))
                builder.color = Color.RED
                builder.setCategory(Notification.CATEGORY_ERROR)
            }

            DownloadStatus.FAILED -> {
                builder.setSmallIcon(R.drawable.ic_download_fail)
                builder.setContentText(context.getString(R.string.download_failed))
                builder.color = Color.RED
                builder.setCategory(Notification.CATEGORY_ERROR)
            }

            DownloadStatus.COMPLETED -> if (progress == 100) {
                builder.setSmallIcon(android.R.drawable.stat_sys_download_done)
                builder.setContentText(context.getString(R.string.download_completed))
                builder.setAutoCancel(true)
                builder.setCategory(Notification.CATEGORY_STATUS)
                builder.setContentIntent(getContentIntentForDetails(context, app))
                builder.addAction(
                    NotificationCompat.Action.Builder(
                        R.drawable.ic_install,
                        context.getString(R.string.action_install),
                        getInstallIntent(context, app.packageName, app.versionCode)
                    ).build()
                )
            }

            DownloadStatus.DOWNLOADING, DownloadStatus.QUEUED -> {
                builder.setSmallIcon(android.R.drawable.stat_sys_download)
                builder.setContentText(
                    if (progress == 0) {
                        context.getString(R.string.download_queued)
                    } else {
                        context.getString(R.string.alt_download_progress)
                    }
                )
                builder.setOngoing(true)
                builder.setCategory(Notification.CATEGORY_PROGRESS)
                builder.setProgress(100, progress, progress == 0)
                builder.foregroundServiceBehavior = NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
                builder.addAction(
                    NotificationCompat.Action.Builder(
                        R.drawable.ic_download_cancel,
                        context.getString(R.string.action_cancel),
                        WorkManager.getInstance(context).createCancelPendingIntent(workID)
                    ).build()
                )
            }

            else -> {}
        }
        return builder.build()
    }

    fun getInstallNotification(context: Context, app: App, content: String?): Notification {
        val builder = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ALERT)
        builder.color = context.getStyledAttributeColor(R.color.colorAccent)
        builder.setSmallIcon(R.drawable.ic_install)
        builder.setContentTitle(app.displayName)
        builder.setContentText(content)
        builder.setSubText(app.packageName)
        return builder.build()
    }

    private fun getPauseIntent(context: Context, groupId: Int): PendingIntent {
        val intent = Intent(context, DownloadPauseReceiver::class.java)
        intent.putExtra(Constants.FETCH_GROUP_ID, groupId)
        val flags = if (isMAndAbove())
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_CANCEL_CURRENT
        return PendingIntent.getBroadcast(context, groupId, intent, flags)
    }

    private fun getResumeIntent(context: Context, groupId: Int): PendingIntent {
        val intent = Intent(context, DownloadResumeReceiver::class.java)
        intent.putExtra(Constants.FETCH_GROUP_ID, groupId)
        val flags = if (isMAndAbove())
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_CANCEL_CURRENT
        return PendingIntent.getBroadcast(context, groupId, intent, flags)
    }

    private fun getCancelIntent(context: Context, groupId: Int): PendingIntent {
        val intent = Intent(context, DownloadCancelReceiver::class.java)
        intent.putExtra(Constants.FETCH_GROUP_ID, groupId)
        val flags = if (isMAndAbove())
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_CANCEL_CURRENT
        return PendingIntent.getBroadcast(context, groupId, intent, flags)
    }

    private fun getContentIntentForDetails(context: Context, app: App?): PendingIntent {
        return NavDeepLinkBuilder(context)
            .setGraph(R.navigation.mobile_navigation)
            .setDestination(R.id.appDetailsFragment)
            .setComponentName(MainActivity::class.java)
            .setArguments(bundleOf("packageName" to app!!.packageName))
            .createPendingIntent()
    }

    private fun getContentIntentForDownloads(context: Context): PendingIntent {
        return NavDeepLinkBuilder(context)
            .setGraph(R.navigation.mobile_navigation)
            .setDestination(R.id.downloadFragment)
            .setComponentName(MainActivity::class.java)
            .createPendingIntent()
    }

    private fun getInstallIntent(
        context: Context,
        packageName: String,
        version: Int
    ): PendingIntent {
        val intent = Intent(context, InstallActivity::class.java).apply {
            putExtra(Constants.STRING_APP, packageName)
            putExtra(Constants.STRING_VERSION, version)
        }
        val flags = if (isMAndAbove()) {
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_CANCEL_CURRENT
        }
        return PendingIntent.getActivity(
            context, packageName.hashCode(), intent, flags
        )
    }
}
