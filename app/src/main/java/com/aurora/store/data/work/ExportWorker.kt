/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.work

import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aurora.extensions.isQAndAbove
import com.aurora.store.data.room.download.Download
import com.aurora.store.data.room.download.DownloadDao
import com.aurora.store.util.NotificationUtil
import com.aurora.store.util.PathUtil
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * An expedited worker to export downloaded apps to a user-chosen URI as a zip bundle.
 */
@HiltWorker
class ExportWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val downloadDao: DownloadDao
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "ExportWorker"

        private const val URI = "URI"
        private const val PACKAGE_NAME = "PACKAGE_NAME"
        private const val VERSION_CODE = "VERSION_CODE"
        private const val DISPLAY_NAME = "DISPLAY_NAME"

        private const val NOTIFICATION_ID = 500
        private const val NOTIFICATION_ID_FGS = 501

        // Large copy buffer to keep throughput high when bundling multi-GB exports
        private const val BUFFER_SIZE = 1024 * 1024

        /**
         * Exports the given download to the URI
         * @param download Download to export
         * @see [ExportWorker]
         */
        fun exportDownloadedApp(context: Context, download: Download, uri: Uri) {
            val inputData = Data.Builder()
                .putString(URI, uri.toString())
                .putString(DISPLAY_NAME, download.displayName)
                .putString(PACKAGE_NAME, download.packageName)
                .putLong(VERSION_CODE, download.versionCode)
                .build()

            val oneTimeWorkRequest = OneTimeWorkRequestBuilder<ExportWorker>()
                .setInputData(inputData)
                .setExpedited(OutOfQuotaPolicy.DROP_WORK_REQUEST)
                .build()

            Log.i(TAG, "Exporting download for ${download.packageName}/${download.versionCode}")
            // Keep any in-progress export for the same app so re-tapping does not queue duplicates
            WorkManager.getInstance(context).enqueueUniqueWork(
                "$TAG/${download.packageName}",
                ExistingWorkPolicy.KEEP,
                oneTimeWorkRequest
            )
        }
    }

    private lateinit var notificationManager: NotificationManager

    override suspend fun doWork(): Result {
        val uri = inputData.getString(URI)!!.toUri()
        val packageName = inputData.getString(PACKAGE_NAME)
        val displayName = inputData.getString(DISPLAY_NAME)
        val versionCode = inputData.getLong(VERSION_CODE, -1)

        notificationManager = context.getSystemService<NotificationManager>()!!

        if (packageName.isNullOrEmpty() || versionCode == -1L) {
            Log.e(TAG, "Input data is corrupt, bailing out!")
            return fail(displayName ?: String(), uri)
        }

        try {
            val download = downloadDao.getDownload(packageName)

            // The download row is keyed by package name only, so guard against it having
            // been replaced by a manual download of a different version between enqueue and
            // execution; the old version's file list is no longer available to export.
            if (download.versionCode != versionCode) {
                Log.e(TAG, "Download for $packageName is no longer version $versionCode, bailing!")
                return fail(displayName ?: packageName, uri)
            }

            val entries = PathUtil.getExportableFiles(context, download)
            if (entries.isEmpty()) {
                // Nothing on disk to bundle, e.g. files were auto-deleted after install
                Log.e(TAG, "No files to export for $packageName, was it auto-deleted?")
                return fail(displayName ?: packageName, uri)
            }

            bundleFiles(displayName ?: packageName, entries, uri)
            notifyStatus(displayName ?: packageName, uri)
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to export $packageName", exception)
            return fail(displayName ?: packageName, uri)
        }

        return Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo =
        exportForegroundInfo(inputData.getString(DISPLAY_NAME))

    private fun exportForegroundInfo(displayName: String?, progress: Int = -1): ForegroundInfo {
        val notification = NotificationUtil.getExportNotification(context, displayName, progress)
        return if (isQAndAbove) {
            ForegroundInfo(NOTIFICATION_ID_FGS, notification, FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID_FGS, notification)
        }
    }

    /**
     * Deletes the partially written (or empty) export document and notifies failure, so a
     * failed export does not leave a corrupt zip behind at the user-chosen location.
     */
    private fun fail(packageName: String, uri: Uri): Result {
        runCatching { DocumentsContract.deleteDocument(context.contentResolver, uri) }
            .onFailure { Log.w(TAG, "Failed to delete incomplete export at $uri", it) }
        notifyStatus(packageName, uri, false)
        return Result.failure()
    }

    private fun notifyStatus(packageName: String, uri: Uri, success: Boolean = true) {
        notificationManager.notify(
            NOTIFICATION_ID,
            NotificationUtil.getExportStatusNotification(
                context,
                packageName,
                uri,
                success
            )
        )
    }

    /**
     * Updates the ongoing foreground notification to reflect export [progress] (0-100).
     */
    private suspend fun notifyProgress(displayName: String, progress: Int) {
        setForeground(exportForegroundInfo(displayName, progress))
    }

    /**
     * Bundles the given files into a zip written to the [uri], preserving each file's
     * relative path within the archive so shared libraries and OBB/patch files retain
     * their layout, while reporting progress through the foreground notification.
     *
     * Entries are stored without compression: APKs and OBBs are already compressed, so
     * deflating them wastes CPU for no size gain and makes large bundles needlessly slow.
     * @param displayName App name shown in the progress notification
     * @param entries Map of zip entry name to the file to write
     * @param uri [Uri] of the file to write the bundle to
     */
    private suspend fun bundleFiles(displayName: String, entries: Map<String, File>, uri: Uri) {
        val totalBytes = entries.values.sumOf { it.length() }.coerceAtLeast(1)
        var writtenBytes = 0L
        var lastProgress = -1

        val output = context.contentResolver.openOutputStream(uri)!!.buffered(BUFFER_SIZE)
        ZipOutputStream(output).use { zipOutput ->
            zipOutput.setLevel(Deflater.NO_COMPRESSION)
            val buffer = ByteArray(BUFFER_SIZE)
            entries.forEach { (entryName, file) ->
                file.inputStream().buffered(BUFFER_SIZE).use { input ->
                    zipOutput.putNextEntry(ZipEntry(entryName))
                    var read = input.read(buffer)
                    while (read >= 0) {
                        zipOutput.write(buffer, 0, read)
                        writtenBytes += read

                        // Throttle to whole-percent changes to avoid being rate-limited
                        val progress = ((writtenBytes * 100) / totalBytes).toInt()
                        if (progress > lastProgress) {
                            lastProgress = progress
                            notifyProgress(displayName, progress)
                        }
                        read = input.read(buffer)
                    }
                    zipOutput.closeEntry()
                }
            }
        }
    }
}
