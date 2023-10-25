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
import com.aurora.Constants
import com.aurora.extensions.getStyledAttributeColor
import com.aurora.extensions.isMAndAbove
import com.aurora.gplayapi.data.models.App
import com.aurora.store.MainActivity
import com.aurora.store.R
import com.aurora.store.data.receiver.DownloadCancelReceiver
import com.aurora.store.data.receiver.DownloadPauseReceiver
import com.aurora.store.data.receiver.DownloadResumeReceiver
import com.aurora.store.data.receiver.InstallReceiver
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.FetchGroup
import com.tonyodev.fetch2.Status

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

    private fun getInstallIntent(context: Context, packageName: String): PendingIntent {
        val intent = Intent(context, InstallReceiver::class.java)
        intent.putExtra(Constants.STRING_EXTRA, packageName)
        val flags = if (isMAndAbove())
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_CANCEL_CURRENT
        return PendingIntent.getBroadcast(
            context,
            packageName.hashCode(),
            intent,
            flags
        )
    }
}
