package com.aurora.store.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import androidx.navigation.NavDeepLinkBuilder
import com.aurora.Constants
import com.aurora.store.MainActivity
import com.aurora.store.R
import com.aurora.store.data.activity.InstallActivity
import com.aurora.store.data.helper.DownloadHelper
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.data.model.DownloadStatus
import com.aurora.store.data.receiver.DownloadCancelReceiver
import com.aurora.store.data.room.download.Download
import com.aurora.store.data.room.update.Update
import java.util.UUID
import kotlin.math.absoluteValue
import com.aurora.store.data.room.download.Download as AuroraDownload

object NotificationUtil {

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService<NotificationManager>()
            val channels = ArrayList<NotificationChannel>()
            channels.add(
                NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_INSTALL,
                    context.getString(R.string.notification_channel_install),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    setSound(null, null)
                }
            )
            channels.add(
                NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_EXPORT,
                    context.getString(R.string.notification_channel_export),
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
            channels.add(
                NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_ACCOUNT,
                    context.getString(R.string.notification_channel_account),
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
            channels.add(
                NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_DOWNLOADS,
                    context.getString(R.string.notification_channel_downloads),
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
            notificationManager!!.createNotificationChannels(channels)
        }
    }

    fun getDownloadNotification(context: Context): Notification {
        return NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_DOWNLOADS)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(context.getString(R.string.app_updater_service_notif_title))
            .setContentText(context.getString(R.string.app_updater_service_notif_text))
            .setOngoing(true)
            .build()
    }

    fun getDownloadNotification(
        context: Context,
        download: AuroraDownload,
        largeIcon: Bitmap? = null
    ): Notification {
        val builder = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_DOWNLOADS)
        builder.setSmallIcon(R.drawable.ic_notification_outlined)
        builder.setContentTitle(download.displayName)
        builder.setContentIntent(getContentIntentForDownloads(context))
        builder.setLargeIcon(largeIcon)

        val cancelIntent = Intent(context, DownloadCancelReceiver::class.java).apply {
            putExtra(DownloadHelper.PACKAGE_NAME, download.packageName)
        }

        val pendingCancelIntent = PendingIntentCompat.getBroadcast(
            context,
            download.packageName.hashCode().absoluteValue,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT,
            false
        )

        when (download.downloadStatus) {
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

                // Show install action if app cannot be silently installed
                if (!AppInstaller.canInstallSilently(
                        context,
                        download.packageName,
                        download.targetSdk
                    )
                ) {
                    builder.addAction(
                        NotificationCompat.Action.Builder(
                            R.drawable.ic_install,
                            context.getString(R.string.action_install),
                            getInstallIntent(context, download)
                        ).build()
                    )
                }
            }

            DownloadStatus.DOWNLOADING, DownloadStatus.QUEUED -> {
                builder.setSmallIcon(android.R.drawable.stat_sys_download)
                builder.setContentText(
                    if (download.progress <= 0) {
                        context.getString(R.string.download_queued)
                    } else {
                        context.getString(
                            R.string.download_progress,
                            download.downloadedFiles + 1,
                            download.totalFiles,
                            CommonUtil.humanReadableByteSpeed(download.speed, true)
                        )
                    }
                )
                builder.setOngoing(true)
                builder.setCategory(Notification.CATEGORY_PROGRESS)
                builder.setProgress(100, download.progress, download.progress <= 0)
                builder.foregroundServiceBehavior = NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
                builder.addAction(
                    NotificationCompat.Action.Builder(
                        R.drawable.ic_download_cancel,
                        context.getString(R.string.action_cancel),
                        pendingCancelIntent
                    ).build()
                )
            }

            else -> {}
        }
        return builder.build()
    }

    fun getInstallNotification(
        context: Context,
        displayName: String,
        packageName: String
    ): Notification {
        return NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_INSTALL)
            .setSmallIcon(R.drawable.ic_install)
            .setLargeIcon(PackageUtil.getIconForPackage(context, packageName))
            .setContentTitle(displayName)
            .setContentText(context.getString(R.string.installer_status_success))
            .setContentIntent(getContentIntentForDetails(context, packageName))
            .build()
    }

    fun getInstallerStatusNotification(
        context: Context,
        packageName: String,
        displayName: String,
        content: String?
    ): Notification {
        return NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_INSTALL)
            .setSmallIcon(R.drawable.ic_install)
            .setContentTitle(displayName)
            .setContentText(content)
            .setContentIntent(getContentIntentForDetails(context, packageName))
            .build()
    }

    fun getUpdateNotification(context: Context): Notification {
        return NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_updates)
            .setContentTitle(context.getString(R.string.checking_updates))
            .setOngoing(true)
            .build()
    }

    fun getUpdateNotification(context: Context, updatesList: List<Update>): Notification {
        val contentIntent = NavDeepLinkBuilder(context)
            .setGraph(R.navigation.mobile_navigation)
            .setDestination(R.id.splashFragment)
            .setComponentName(MainActivity::class.java)
            .setArguments(bundleOf("destinationId" to R.id.updatesFragment))
            .createPendingIntent()

        return NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_updates)
            .setContentTitle(
                if (updatesList.size == 1)
                    context.getString(
                        R.string.notification_updates_available_1,
                        updatesList.size
                    )
                else
                    context.getString(
                        R.string.notification_updates_available,
                        updatesList.size
                    )
            )
            .setContentText(
                when (updatesList.size) {
                    1 -> {
                        context.getString(
                            R.string.notification_updates_available_desc_1,
                            updatesList[0].displayName
                        )
                    }

                    2 -> {
                        context.getString(
                            R.string.notification_updates_available_desc_2,
                            updatesList[0].displayName,
                            updatesList[1].displayName
                        )
                    }

                    3 -> {
                        context.getString(
                            R.string.notification_updates_available_desc_3,
                            updatesList[0].displayName,
                            updatesList[1].displayName,
                            updatesList[2].displayName
                        )
                    }

                    else -> {
                        context.getString(
                            R.string.notification_updates_available_desc_4,
                            updatesList[0].displayName,
                            updatesList[1].displayName,
                            updatesList[2].displayName,
                            updatesList.size - 3
                        )
                    }
                }
            )
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setAutoCancel(true)
            .build()
    }

    fun getExportNotification(context: Context): Notification {
        return NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_EXPORT)
            .setSmallIcon(R.drawable.ic_file_copy)
            .setContentTitle(context.getString(R.string.export_app_title))
            .setContentText(context.getString(R.string.export_app_summary))
            .setOngoing(true)
            .build()
    }

    fun getUnarchiveAuthNotification(context: Context, packageName: String): Notification {
        return NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ACCOUNT)
            .setSmallIcon(R.drawable.ic_account)
            .setContentTitle(context.getString(R.string.authentication_required_title))
            .setContentText(context.getString(R.string.authentication_required_unarchive))
            .setAutoCancel(true)
            .setContentIntent(getContentIntentForSplash(context, packageName))
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    fun getExportStatusNotification(
        context: Context,
        displayName: String,
        uri: Uri,
        success: Boolean
    ): Notification {
        val intent = Intent(Intent.ACTION_SEND).apply {
            setDataAndType(uri, "application/zip")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            putExtra(Intent.EXTRA_STREAM, uri)
        }
        val pendingIntent = PendingIntentCompat.getActivity(
            context,
            UUID.randomUUID().hashCode(),
            Intent.createChooser(intent, null),
            PendingIntent.FLAG_CANCEL_CURRENT,
            false
        )

        val content = if (success) {
            context.getString(R.string.export_app_summary_success)
        } else {
            context.getString(R.string.export_app_summary_fail)
        }

        val builder = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_EXPORT)
            .setSmallIcon(R.drawable.ic_file_copy)
            .setContentTitle(displayName).setContentText(content)
            .setContentIntent(getContentIntentForExport(context, uri))
            .setAutoCancel(true)

        if (success) {
            builder.addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.ic_share,
                    context.getString(R.string.action_share),
                    pendingIntent
                ).build()
            )
        }
        return builder.build()
    }

    private fun getContentIntentForExport(context: Context, uri: Uri): PendingIntent? {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/zip")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        return PendingIntentCompat.getActivity(
            context,
            UUID.randomUUID().hashCode(),
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT,
            false
        )
    }

    private fun getContentIntentForSplash(context: Context, packageName: String): PendingIntent {
        return NavDeepLinkBuilder(context)
            .setGraph(R.navigation.mobile_navigation)
            .setDestination(R.id.splashFragment)
            .setComponentName(MainActivity::class.java)
            .setArguments(bundleOf("packageName" to packageName))
            .createPendingIntent()
    }

    private fun getContentIntentForDetails(context: Context, packageName: String): PendingIntent {
        return NavDeepLinkBuilder(context)
            .setGraph(R.navigation.mobile_navigation)
            .setDestination(R.id.splashFragment)
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

    private fun getInstallIntent(context: Context, download: Download): PendingIntent? {
        val intent = Intent(context, InstallActivity::class.java).apply {
            putExtra(Constants.PARCEL_DOWNLOAD, download)
        }
        return PendingIntentCompat.getActivity(
            context,
            download.packageName.hashCode(),
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT,
            false
        )
    }
}
