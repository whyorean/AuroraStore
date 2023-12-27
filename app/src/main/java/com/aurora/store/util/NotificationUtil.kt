package com.aurora.store.util

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.util.Log
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
import com.aurora.store.data.room.download.Download as AuroraDownload
import com.aurora.store.R
import com.aurora.store.data.activity.InstallActivity
import com.aurora.store.data.model.DownloadStatus
import java.net.URL
import java.util.UUID

object NotificationUtil {

    private const val TAG = "NotificationUtil"

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

    fun getDownloadNotification(context: Context): Notification {
        return NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_GENERAL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setColor(ContextCompat.getColor(context, R.color.colorAccent))
            .setContentTitle(context.getString(R.string.app_updater_service_notif_title))
            .setContentText(context.getString(R.string.app_updater_service_notif_text))
            .setOngoing(true)
            .build()
    }

    fun getDownloadNotification(
        context: Context,
        download: AuroraDownload,
        workID: UUID
    ): Notification {
        val builder = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_GENERAL)
        builder.setContentTitle(download.displayName)
        builder.color = ContextCompat.getColor(context, R.color.colorAccent)
        builder.setContentIntent(getContentIntentForDownloads(context))

        // Set big icon for download
        try {
            val bitmap = BitmapFactory.decodeStream(URL(download.iconURL).openStream())
            builder.setLargeIcon(bitmap)
        } catch (exception: Exception) {
            Log.i(TAG, "Failed to set big icon", exception)
        }

        when (download.status) {
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

            DownloadStatus.COMPLETED -> {
                builder.setSmallIcon(android.R.drawable.stat_sys_download_done)
                builder.setContentText(context.getString(R.string.download_completed))
                builder.setAutoCancel(true)
                builder.setCategory(Notification.CATEGORY_STATUS)
                builder.setContentIntent(getContentIntentForDetails(context, download.packageName))
                builder.addAction(
                    NotificationCompat.Action.Builder(
                        R.drawable.ic_install,
                        context.getString(R.string.action_install),
                        getInstallIntent(context, download.packageName, download.versionCode)
                    ).build()
                )
            }

            DownloadStatus.DOWNLOADING, DownloadStatus.QUEUED -> {
                builder.setSmallIcon(android.R.drawable.stat_sys_download)
                builder.setContentText(
                    if (download.progress == 0) {
                        context.getString(R.string.download_queued)
                    } else {
                        context.getString(
                            R.string.download_progress,
                            download.downloadedFiles,
                            download.totalFiles,
                            CommonUtil.humanReadableByteSpeed(download.speed, true)
                        )
                    }
                )
                builder.setOngoing(true)
                builder.setCategory(Notification.CATEGORY_PROGRESS)
                builder.setProgress(100, download.progress, download.progress == 0)
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

    fun getInstallerStatusNotification(context: Context, app: App, content: String?): Notification {
        val builder =
            NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ALERT).apply {
                color = context.getStyledAttributeColor(R.color.colorAccent)
                setSmallIcon(R.drawable.ic_install)
                setContentTitle(app.displayName)
                setContentText(content)
                setContentIntent(getContentIntentForDetails(context, app.packageName))
                setSubText(app.packageName)
            }
        return builder.build()
    }

    private fun getContentIntentForDetails(context: Context, packageName: String): PendingIntent {
        return NavDeepLinkBuilder(context)
            .setGraph(R.navigation.mobile_navigation)
            .setDestination(R.id.appDetailsFragment)
            .setComponentName(MainActivity::class.java)
            .setArguments(bundleOf("packageName" to packageName))
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
