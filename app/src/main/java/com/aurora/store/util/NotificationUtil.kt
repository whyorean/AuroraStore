package com.aurora.store.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
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
import com.aurora.Constants
import com.aurora.store.ComposeActivity
import com.aurora.store.R
import com.aurora.store.compose.navigation.Screen
import com.aurora.store.data.activity.InstallActivity
import com.aurora.store.data.helper.DownloadHelper
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.data.model.DownloadStatus
import com.aurora.store.data.receiver.DownloadCancelReceiver
import com.aurora.store.data.receiver.DownloadRetryReceiver
import com.aurora.store.data.room.download.Download
import com.aurora.store.data.room.download.Download as AuroraDownload
import com.aurora.store.data.room.update.Update
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

object NotificationUtil {

    // Channel groups: headings the individual channels are filed under in system settings.
    private const val GROUP_CHANNELS_ACTIVITY = "com.aurora.store.channels.ACTIVITY"
    private const val GROUP_CHANNELS_ALERTS = "com.aurora.store.channels.ALERTS"

    // Terminal install/failure notifications are bundled under a group so a bulk update
    // shows a single collapsible summary instead of one notification per app.
    private const val GROUP_INSTALLED = "com.aurora.store.INSTALLED"
    private const val GROUP_FAILED = "com.aurora.store.FAILED"

    // Fixed IDs for the two group summaries. Kept well clear of the per-app IDs
    // (packageName.hashCode()) and the worker IDs (100/200/500/501).
    private const val SUMMARY_ID_INSTALLED = 1_000_001
    private const val SUMMARY_ID_FAILED = 1_000_002

    // Successful installs are informational, so they expire on their own rather than
    // lingering. Failures are actionable and persist until handled.
    private val INSTALLED_TIMEOUT_MS = TimeUnit.HOURS.toMillis(6)

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService<NotificationManager>()!!

            // Drop channels retired or superseded by a lower-importance replacement, so the
            // user no longer sees stale entries in system settings.
            Constants.LEGACY_NOTIFICATION_CHANNELS.forEach {
                notificationManager.deleteNotificationChannel(it)
            }

            // Organise the channels under two headings in system settings: routine activity
            // vs. things that need attention.
            notificationManager.createNotificationChannelGroups(
                listOf(
                    NotificationChannelGroup(
                        GROUP_CHANNELS_ACTIVITY,
                        context.getString(R.string.notification_group_activity)
                    ),
                    NotificationChannelGroup(
                        GROUP_CHANNELS_ALERTS,
                        context.getString(R.string.notification_group_alerts)
                    )
                )
            )

            val channels = ArrayList<NotificationChannel>()

            // Quiet, ongoing progress for active downloads. MIN keeps it collapsed and out
            // of the status bar where possible.
            channels.add(
                NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_DOWNLOADS,
                    context.getString(R.string.notification_channel_downloads),
                    NotificationManager.IMPORTANCE_MIN
                ).apply {
                    group = GROUP_CHANNELS_ACTIVITY
                    setSound(null, null)
                    setShowBadge(false)
                }
            )

            // Silent confirmation that an app finished installing/updating. LOW so it lands
            // in the shade without buzzing for every app during a bulk update.
            channels.add(
                NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_INSTALL,
                    context.getString(R.string.notification_channel_install),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    group = GROUP_CHANNELS_ACTIVITY
                    setSound(null, null)
                }
            )

            // New updates are available to install. DEFAULT but silent.
            channels.add(
                NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_UPDATES,
                    context.getString(R.string.notification_channel_updates),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    group = GROUP_CHANNELS_ACTIVITY
                    setSound(null, null)
                }
            )

            // Background app export. LOW since it's user-initiated and non-urgent.
            channels.add(
                NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_EXPORT,
                    context.getString(R.string.notification_channel_export),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    group = GROUP_CHANNELS_ACTIVITY
                    setSound(null, null)
                }
            )

            // Things the user genuinely needs to act on: failed downloads/installs, pending
            // user action and authentication prompts. HIGH so these aren't missed.
            channels.add(
                NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_ALERTS,
                    context.getString(R.string.notification_channel_alerts),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    group = GROUP_CHANNELS_ALERTS
                }
            )

            notificationManager.createNotificationChannels(channels)
        }
    }

    fun getDownloadNotification(context: Context): Notification =
        NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_DOWNLOADS)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(context.getString(R.string.app_updater_service_notif_title))
            .setContentText(context.getString(R.string.app_updater_service_notif_text))
            .setOngoing(true)
            .build()

    fun getDownloadNotification(
        context: Context,
        download: AuroraDownload,
        largeIcon: Bitmap? = null,
        message: String? = null
    ): Notification {
        // Terminal failures go to the high-importance alerts channel so they aren't lost in
        // the silent downloads channel; everything else stays quiet.
        val channelId = if (download.status == DownloadStatus.FAILED) {
            Constants.NOTIFICATION_CHANNEL_ALERTS
        } else {
            Constants.NOTIFICATION_CHANNEL_DOWNLOADS
        }
        val builder = NotificationCompat.Builder(context, channelId)
        builder.setSmallIcon(R.drawable.ic_notification_outlined)
        builder.setContentTitle(download.displayName)
        builder.setContentIntent(getContentIntentForDetails(context, download.packageName))
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

        when (download.status) {
            DownloadStatus.CANCELLED -> {
                builder.setSmallIcon(R.drawable.ic_download_cancel)
                builder.setContentText(context.getString(R.string.download_canceled))
                builder.color = Color.RED
                builder.setCategory(Notification.CATEGORY_ERROR)
            }

            DownloadStatus.FAILED -> {
                builder.setSmallIcon(R.drawable.ic_download_fail)
                builder.setContentText(message ?: context.getString(R.string.download_failed))
                builder.color = Color.RED
                builder.setCategory(Notification.CATEGORY_ERROR)
                builder.setGroup(GROUP_FAILED)
                builder.setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                builder.addAction(getRetryAction(context, download.packageName))
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

            DownloadStatus.INSTALLING -> {
                builder.setSmallIcon(android.R.drawable.stat_sys_download_done)
                builder.setContentText(context.getString(R.string.status_installing))
                builder.setOngoing(true)
                builder.setCategory(Notification.CATEGORY_PROGRESS)
                builder.setProgress(100, 100, true)
                builder.foregroundServiceBehavior = NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
            }

            else -> {}
        }
        return builder.build()
    }

    fun getInstallNotification(
        context: Context,
        displayName: String,
        packageName: String
    ): Notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_INSTALL)
        .setSmallIcon(R.drawable.ic_install)
        .setLargeIcon(PackageUtil.getIconForPackage(context, packageName))
        .setContentTitle(displayName)
        .setContentText(context.getString(R.string.installer_status_success))
        .setContentIntent(getContentIntentForDetails(context, packageName))
        .setCategory(Notification.CATEGORY_STATUS)
        .setGroup(GROUP_INSTALLED)
        .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
        .setAutoCancel(true)
        .setTimeoutAfter(INSTALLED_TIMEOUT_MS)
        .build()

    fun getInstallerStatusNotification(
        context: Context,
        packageName: String,
        displayName: String,
        content: String?
    ): Notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ALERTS)
        .setSmallIcon(R.drawable.ic_install)
        .setContentTitle(displayName)
        .setContentText(content)
        .setContentIntent(getContentIntentForDetails(context, packageName))
        .setCategory(Notification.CATEGORY_ERROR)
        .setGroup(GROUP_FAILED)
        .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
        .addAction(getRetryAction(context, packageName))
        .build()

    /**
     * Posts the grouped "app installed" notification and refreshes the group summary so a
     * bulk update collapses into a single "N apps installed" entry.
     */
    fun notifyInstalled(context: Context, displayName: String, packageName: String) {
        val notificationManager = context.getSystemService<NotificationManager>()!!
        notificationManager.notify(
            packageName.hashCode(),
            getInstallNotification(context, displayName, packageName)
        )
        refreshGroupSummaries(context)
    }

    /**
     * Posts the grouped install-failure notification (with a retry action) and refreshes the
     * failure group summary.
     */
    fun notifyInstallFailed(
        context: Context,
        packageName: String,
        displayName: String,
        content: String?
    ) {
        val notificationManager = context.getSystemService<NotificationManager>()!!
        notificationManager.notify(
            packageName.hashCode(),
            getInstallerStatusNotification(context, packageName, displayName, content)
        )
        refreshGroupSummaries(context)
    }

    /**
     * Cancels the per-app notification for [packageName] (e.g. once the user retries a failed
     * install) and reconciles the group summaries.
     */
    fun clearAppNotification(context: Context, packageName: String) {
        context.getSystemService<NotificationManager>()!!.cancel(packageName.hashCode())
        refreshGroupSummaries(context)
    }

    private fun getRetryAction(context: Context, packageName: String): NotificationCompat.Action {
        val intent = Intent(context, DownloadRetryReceiver::class.java).apply {
            putExtra(DownloadHelper.PACKAGE_NAME, packageName)
        }
        val pendingIntent = PendingIntentCompat.getBroadcast(
            context,
            packageName.hashCode().absoluteValue,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT,
            false
        )
        return NotificationCompat.Action.Builder(
            R.drawable.ic_updates,
            context.getString(R.string.action_retry),
            pendingIntent
        ).build()
    }

    /**
     * Rebuilds the two group summaries from the currently-posted per-app notifications. Each
     * summary lists its apps via [NotificationCompat.InboxStyle] and is removed once no
     * children remain. Reading the live notifications (rather than tracking state ourselves)
     * keeps the summaries correct across retries, dismissals and process restarts.
     */
    fun refreshGroupSummaries(context: Context) {
        // Grouping/summaries are only rendered from Android N onwards; on older versions a
        // summary would just show as an extra standalone notification, so leave the children
        // to display individually.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return

        val notificationManager = context.getSystemService<NotificationManager>()!!

        refreshSummary(
            notificationManager = notificationManager,
            context = context,
            group = GROUP_INSTALLED,
            summaryId = SUMMARY_ID_INSTALLED,
            channelId = Constants.NOTIFICATION_CHANNEL_INSTALL,
            smallIcon = R.drawable.ic_install,
            titleRes = R.plurals.notification_installed_summary,
            timeoutMs = INSTALLED_TIMEOUT_MS,
            contentIntent = getContentIntentForMain(context, initialTab = 2)
        )
        refreshSummary(
            notificationManager = notificationManager,
            context = context,
            group = GROUP_FAILED,
            summaryId = SUMMARY_ID_FAILED,
            channelId = Constants.NOTIFICATION_CHANNEL_ALERTS,
            smallIcon = R.drawable.ic_download_fail,
            titleRes = R.plurals.notification_failed_summary,
            timeoutMs = null,
            contentIntent = getContentIntentForMain(context, initialTab = 2)
        )
    }

    @Suppress("LongParameterList")
    private fun refreshSummary(
        notificationManager: NotificationManager,
        context: Context,
        group: String,
        summaryId: Int,
        channelId: String,
        smallIcon: Int,
        titleRes: Int,
        timeoutMs: Long?,
        contentIntent: PendingIntent?
    ) {
        val children = notificationManager.activeNotifications.filter {
            it.notification.group == group && it.id != summaryId
        }

        if (children.isEmpty()) {
            notificationManager.cancel(summaryId)
            return
        }

        val titles = children.mapNotNull {
            it.notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        }
        val title = context.resources.getQuantityString(titleRes, children.size, children.size)

        val inboxStyle = NotificationCompat.InboxStyle().setBigContentTitle(title)
        titles.forEach { inboxStyle.addLine(it) }

        val summary = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(smallIcon)
            .setContentTitle(title)
            .setContentText(titles.joinToString(", "))
            .setStyle(inboxStyle)
            .setGroup(group)
            .setGroupSummary(true)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .apply { timeoutMs?.let { setTimeoutAfter(it) } }
            .build()

        notificationManager.notify(summaryId, summary)
    }

    fun getUpdateNotification(context: Context): Notification =
        NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_updates)
            .setContentTitle(context.getString(R.string.checking_updates))
            .setOngoing(true)
            .build()

    fun getUpdateNotification(context: Context, updatesList: List<Update>): Notification {
        val contentIntent = getContentIntentForMain(context, initialTab = 2)

        return NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_updates)
            .setContentTitle(
                if (updatesList.size == 1) {
                    context.getString(
                        R.string.notification_updates_available_1,
                        updatesList.size
                    )
                } else {
                    context.getString(
                        R.string.notification_updates_available,
                        updatesList.size
                    )
                }
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

    fun getExportNotification(
        context: Context,
        displayName: String? = null,
        progress: Int = -1
    ): Notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_EXPORT)
        .setSmallIcon(R.drawable.ic_file_copy)
        .setContentTitle(displayName ?: context.getString(R.string.export_app_title))
        .setContentText(context.getString(R.string.export_app_summary))
        .setProgress(100, progress.coerceAtLeast(0), progress < 0)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setCategory(Notification.CATEGORY_PROGRESS)
        .setSilent(true)
        .build()

    fun getUnarchiveAuthNotification(context: Context, packageName: String): Notification =
        NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_account)
            .setContentTitle(context.getString(R.string.authentication_required_title))
            .setContentText(context.getString(R.string.authentication_required_unarchive))
            .setAutoCancel(true)
            .setContentIntent(getContentIntentForDetails(context, packageName))
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

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

    private fun getContentIntentForDetails(context: Context, packageName: String): PendingIntent? {
        val intent = Intent(context, ComposeActivity::class.java).apply {
            putExtra(Screen.PARCEL_KEY, Screen.AppDetails(packageName) as android.os.Parcelable)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntentCompat.getActivity(
            context,
            packageName.hashCode().absoluteValue,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT,
            false
        )
    }

    private fun getContentIntentForMain(context: Context, initialTab: Int): PendingIntent? {
        val intent = Intent(context, ComposeActivity::class.java).apply {
            putExtra(Screen.PARCEL_KEY, Screen.Main(initialTab) as android.os.Parcelable)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntentCompat.getActivity(
            context,
            initialTab,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT,
            false
        )
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
