package com.aurora.store.data.work

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy.KEEP
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aurora.Constants
import com.aurora.extensions.copyAndAdd
import com.aurora.extensions.copyTo
import com.aurora.extensions.isQAndAbove
import com.aurora.extensions.copyAndRemove
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.helpers.PurchaseHelper
import com.aurora.store.AuroraApplication
import com.aurora.store.data.model.DownloadStatus
import com.aurora.store.data.model.Request
import com.aurora.store.data.network.HttpClient
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.data.receiver.InstallReceiver
import com.aurora.store.util.NotificationUtil
import com.aurora.store.util.PathUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import kotlinx.coroutines.flow.update
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import com.aurora.gplayapi.data.models.File as GPlayFile

class DownloadWorker(private val appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    companion object {
        const val DOWNLOAD_WORKER = "DOWNLOAD_WORKER"
        const val DOWNLOAD_PROGRESS = "DOWNLOAD_PROGRESS"

        fun enqueueApp(app: App) {
            AuroraApplication.enqueuedDownloads.update { it.copyAndAdd(app) }
        }

        /**
         * Downloads and install an [App]
         *
         * Triggers Immediate downloads and installation. In most cases, you don't need to call
         * this method. Consider using [enqueueApp] instead.
         */
        fun downloadApp(context: Context, app: App) {
            val work = OneTimeWorkRequestBuilder<DownloadWorker>()
                .addTag(app.packageName)
                .addTag(app.versionCode.toString())
                .setExpedited(OutOfQuotaPolicy.DROP_WORK_REQUEST)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(DOWNLOAD_WORKER, KEEP, work)
        }
    }

    private lateinit var app: App
    private lateinit var notificationManager: NotificationManager
    private var downloading = false

    private val TAG = DownloadWorker::class.java.simpleName
    private val notificationID = 200

    override suspend fun doWork(): Result {
        // Purchase the app (free apps needs to be purchased too)
        val authData = AuthProvider.with(appContext).getAuthData()
        val purchaseHelper = PurchaseHelper(authData)
            .using(HttpClient.getPreferredClient(appContext))

        notificationManager =
            appContext.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager

        // Try to parse input data into a valid app
        try {
            app = AuroraApplication.enqueuedDownloads.value.first()
        } catch (exception: Exception) {
            Log.e(TAG, "No apps enqueued for downloads", exception)
            return Result.failure()
        }

        // Set work/service to foreground on < Android 12.0
        setForeground(getForegroundInfo())

        // Bail out if file list is empty
        val files = purchaseHelper.purchase(app.packageName, app.versionCode, app.offerType)
        if (files.isEmpty()) {
            Log.i(TAG, "Nothing to download!")
            notifyStatus(DownloadStatus.COMPLETED)
            return Result.success()
        }

        // Download and verify all files exists
        PathUtil.getAppDownloadDir(appContext, app.packageName, app.versionCode).createDirectories()

        val requestList = getDownloadRequest(files)
        requestList.forEach { request ->
            downloading = true
            runCatching { downloadFile(request) }
                .onSuccess { downloading = false }
                .onFailure {
                    Log.e(TAG, "Failed to download ${app.packageName}", it)
                    downloading = false
                    onFailure()
                    return Result.failure()
                }
            while (downloading) {
                delay(1000)
                if (isStopped) {
                    onFailure()
                    break
                }
            }
        }

        if (!requestList.all { File(it.filePath).exists() }) return Result.failure()

        // Mark download as completed
        notifyStatus(DownloadStatus.COMPLETED)
        Log.i(TAG, "Finished downloading ${app.packageName}")

        // Notify for installation
        Intent(appContext, InstallReceiver::class.java).also {
            it.action = InstallReceiver.ACTION_INSTALL_APP
            it.putExtra(Constants.STRING_APP, app.packageName)
            it.putExtra(Constants.STRING_VERSION, app.versionCode)
            appContext.sendBroadcast(it)
        }

        // Remove the app from the list
        AuroraApplication.enqueuedDownloads.update { it.copyAndRemove(app) }
        return Result.success()
    }

    @OptIn(ExperimentalPathApi::class)
    private fun onFailure() {
        Log.i(TAG, "Cleaning up!")
        PathUtil.getAppDownloadDir(appContext, app.packageName, app.versionCode)
            .deleteRecursively()
        notificationManager.cancel(notificationID)
        AuroraApplication.enqueuedDownloads.update { it.copyAndRemove(app) }
    }

    private fun getDownloadRequest(files: List<GPlayFile>): List<Request> {
        val downloadList = mutableListOf<Request>()
        files.filter { it.url.isNotBlank() }.forEach {
            val filePath = when (it.type) {
                GPlayFile.FileType.BASE,
                GPlayFile.FileType.SPLIT -> PathUtil.getApkDownloadFile(appContext, app, it)

                GPlayFile.FileType.OBB,
                GPlayFile.FileType.PATCH -> PathUtil.getObbDownloadFile(app, it)
            }
            downloadList.add(Request(it.url, filePath, it.size))
        }
        return downloadList
    }

    private suspend fun downloadFile(request: Request): Result {
        return withContext(Dispatchers.IO) {
            val requestFile = File(request.filePath)
            try {
                requestFile.createNewFile()
                URL(request.url).openStream().use { input ->
                    requestFile.outputStream().use {
                        input.copyTo(it, request.size).collectLatest { p -> onProgress(p) }
                    }
                }
                // Ensure downloaded file exists
                if (!File(request.filePath).exists()) {
                    Log.e(TAG, "Failed to find downloaded file at ${request.filePath}")
                    notifyStatus(DownloadStatus.FAILED)
                    return@withContext Result.failure()
                }
                return@withContext Result.success()
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to download ${request.filePath}!", exception)
                requestFile.delete()
                notifyStatus(DownloadStatus.FAILED)
                return@withContext Result.failure()
            }
        }
    }

    private suspend fun onProgress(progress: Int) {
        if (!isStopped) {
            setProgress(Data.Builder().putInt(DOWNLOAD_PROGRESS, progress).build())
            notifyStatus(DownloadStatus.DOWNLOADING, progress, notificationID)
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = NotificationUtil.getDownloadNotification(
            appContext,
            app,
            DownloadStatus.QUEUED,
            0,
            id
        )
        return if (isQAndAbove()) {
            ForegroundInfo(notificationID, notification, FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationID, notification)
        }
    }

    private fun notifyStatus(status: DownloadStatus, progress: Int = 100, dID: Int = -1) {
        val notification =
            NotificationUtil.getDownloadNotification(appContext, app, status, progress, id)
        notificationManager.notify(if (dID != -1) dID else app.packageName.hashCode(), notification)
    }
}
