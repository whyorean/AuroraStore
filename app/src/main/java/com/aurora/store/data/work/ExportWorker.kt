package com.aurora.store.data.work

import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.content.getSystemService
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aurora.store.data.model.MinimalApp
import com.aurora.store.data.room.download.Download
import com.aurora.store.util.NotificationUtil
import com.aurora.store.util.PackageUtil.getPackageInfo
import com.aurora.store.util.PathUtil
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * An expedited worker to export downloaded or installed apps.
 */
@HiltWorker
class ExportWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "ExportWorker"

        private const val URI = "URI"
        private const val IS_DOWNLOAD = "IS_DOWNLOAD"
        private const val PACKAGE_NAME = "PACKAGE_NAME"
        private const val VERSION_CODE = "VERSION_CODE"
        private const val DISPLAY_NAME = "DISPLAY_NAME"

        /**
         * Exports the installed package to the given URI
         * @param app App to export
         * @see [ExportWorker]
         */
        fun exportInstalledApp(context: Context, app: MinimalApp, uri: Uri) {
            val inputData = Data.Builder()
                .putBoolean(IS_DOWNLOAD, false)
                .putString(URI, uri.toString())
                .putString(DISPLAY_NAME, app.displayName)
                .putString(PACKAGE_NAME, app.packageName)
                .build()

            val oneTimeWorkRequest = OneTimeWorkRequestBuilder<ExportWorker>()
                .setInputData(inputData)
                .setExpedited(OutOfQuotaPolicy.DROP_WORK_REQUEST)
                .build()

            Log.i(TAG, "Exporting $app.packageName")
            WorkManager.getInstance(context).enqueue(oneTimeWorkRequest)
        }

        /**
         * Exports the given download to the URI
         * @param download Download to export
         * @see [ExportWorker]
         */
        fun exportDownloadedApp(context: Context, download: Download, uri: Uri) {
            val inputData = Data.Builder()
                .putBoolean(IS_DOWNLOAD, true)
                .putString(URI, uri.toString())
                .putString(DISPLAY_NAME, download.displayName)
                .putString(PACKAGE_NAME, download.packageName)
                .putInt(VERSION_CODE, download.versionCode)
                .build()

            val oneTimeWorkRequest = OneTimeWorkRequestBuilder<ExportWorker>()
                .setInputData(inputData)
                .setExpedited(OutOfQuotaPolicy.DROP_WORK_REQUEST)
                .build()

            Log.i(TAG, "Exporting download for ${download.packageName}/${download.versionCode}")
            WorkManager.getInstance(context).enqueue(oneTimeWorkRequest)
        }
    }

    private lateinit var notificationManager: NotificationManager
    private val NOTIFICATION_ID = 500

    override suspend fun doWork(): Result {
        val isDownload = inputData.getBoolean(IS_DOWNLOAD, false)
        val uri = Uri.parse(inputData.getString(URI))
        val packageName = inputData.getString(PACKAGE_NAME)
        val displayName = inputData.getString(DISPLAY_NAME)
        val versionCode = inputData.getInt(VERSION_CODE, -1)

        notificationManager = appContext.getSystemService<NotificationManager>()!!

        if (packageName.isNullOrEmpty() || isDownload && versionCode == -1) {
            Log.e(TAG, "Input data is corrupt, bailing out!")
            notifyStatus(displayName ?: String(), uri, false)
            return Result.failure()
        }

        try {
            if (isDownload) {
                copyDownloadedApp(packageName, versionCode, uri)
            } else {
                copyInstalledApp(packageName, uri)
            }
            notifyStatus(displayName ?: packageName, uri)
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to export $packageName", exception)
            notifyStatus(displayName ?: packageName, uri, false)
            return Result.failure()
        }

        return Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            NOTIFICATION_ID,
            NotificationUtil.getExportNotification(appContext)
        )
    }

    private fun notifyStatus(packageName: String, uri: Uri, success: Boolean = true) {
        notificationManager.notify(
            NOTIFICATION_ID,
            NotificationUtil.getExportStatusNotification(
                appContext,
                packageName,
                uri,
                success
            )
        )
    }

    private fun copyInstalledApp(packageName: String, uri: Uri) {
        val packageInfo = getPackageInfo(appContext, packageName, PackageManager.GET_META_DATA)
        val fileList: MutableList<File?> = mutableListOf()

        fileList.add(File(packageInfo.applicationInfo!!.sourceDir))
        packageInfo.applicationInfo!!.splitSourceDirs?.let { splits ->
            fileList.addAll(splits.map { File(it) })
        }

        bundleAllAPKs(fileList.filterNotNull(), uri)
    }

    private fun copyDownloadedApp(packageName: String, versionCode: Int, uri: Uri) {
        return bundleAllAPKs(
            PathUtil.getAppDownloadDir(appContext, packageName, versionCode).listFiles()!!.toList(),
            uri
        )
    }

    /**
     * Bundles all the given APKs to a zip file
     * @param fileList List of APKs to add to the zip
     * @param uri [Uri] of the file to write the APKs
     */
    private fun bundleAllAPKs(fileList: List<File>, uri: Uri) {
        ZipOutputStream(appContext.contentResolver.openOutputStream(uri)).use { zipOutput ->
            fileList.forEach { file ->
                file.inputStream().use { input ->
                    val zipEntry = ZipEntry(file.name)
                    zipOutput.putNextEntry(zipEntry)
                    input.copyTo(zipOutput)
                    zipOutput.closeEntry()
                }
            }
        }
    }
}
