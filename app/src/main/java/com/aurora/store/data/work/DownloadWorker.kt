package com.aurora.store.data.work

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.aurora.Constants
import com.aurora.extensions.copyTo
import com.aurora.extensions.isQAndAbove
import com.aurora.gplayapi.helpers.PurchaseHelper
import com.aurora.store.data.model.DownloadInfo
import com.aurora.store.data.model.DownloadStatus
import com.aurora.store.data.model.Request
import com.aurora.store.data.network.HttpClient
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.data.receiver.InstallReceiver
import com.aurora.store.data.room.download.Download
import com.aurora.store.data.room.download.DownloadDao
import com.aurora.store.util.DownloadWorkerUtil
import com.aurora.store.util.NotificationUtil
import com.aurora.store.util.PathUtil
import com.google.gson.Gson
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import kotlinx.coroutines.NonCancellable
import kotlin.properties.Delegates
import com.aurora.gplayapi.data.models.File as GPlayFile

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    private val downloadDao: DownloadDao,
    private val gson: Gson,
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private lateinit var download: Download
    private lateinit var notificationManager: NotificationManager
    private var downloading = false

    private val NOTIFICATION_ID = 200

    private var totalBytes by Delegates.notNull<Long>()
    private var totalProgress = 0
    private var downloadedBytes = 0L

    private val TAG = DownloadWorker::class.java.simpleName

    override suspend fun doWork(): Result {
        // Try to parse input data into a valid app
        try {
            val downloadData = inputData.getString(DownloadWorkerUtil.DOWNLOAD_DATA)
            download = gson.fromJson(downloadData, Download::class.java)
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to parse download data", exception)
            return Result.failure()
        }

        // Set work/service to foreground on < Android 12.0
        setForeground(getForegroundInfo())

        // Purchase the app (free apps needs to be purchased too)
        val authData = AuthProvider.with(appContext).getAuthData()
        val purchaseHelper = PurchaseHelper(authData)
            .using(HttpClient.getPreferredClient(appContext))

        notificationManager =
            appContext.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager

        // Bail out if file list is empty
        val files =
            purchaseHelper.purchase(download.packageName, download.versionCode, download.offerType)
        if (files.isEmpty()) {
            Log.i(TAG, "Nothing to download!")
            notifyStatus(DownloadStatus.FAILED)
            return Result.failure()
        }

        // Create dirs & generate download request for files and shared libs (if any)
        PathUtil.getAppDownloadDir(appContext, download.packageName, download.versionCode).mkdirs()
        if (files.any { it.type == GPlayFile.FileType.OBB || it.type == GPlayFile.FileType.PATCH }) {
            PathUtil.getObbDownloadDir(download.packageName).mkdirs()
        }

        val requestList = mutableListOf<Request>()
        if (download.sharedLibs.isNotEmpty()) {
            // Purchase and append shared libs data to existing request
            download.sharedLibs.forEach {
                PathUtil.getLibDownloadDir(
                    appContext,
                    download.packageName,
                    download.versionCode,
                    it.packageName
                ).mkdirs()
                val libs = purchaseHelper.purchase(it.packageName, it.versionCode, 0)
                requestList.addAll(getDownloadRequest(libs, it.packageName))
            }
        }
        requestList.addAll(getDownloadRequest(files, null))

        // Update data for notification
        download.totalFiles = requestList.size
        totalBytes = requestList.sumOf { it.size }

        // Download and verify all files exists
        requestList.forEach { request ->
            downloading = true
            runCatching { downloadFile(request); download.downloadedFiles++ }
                .onSuccess { downloading = false }
                .onFailure {
                    Log.e(TAG, "Failed to download ${download.packageName}", it)
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
        Log.i(TAG, "Finished downloading ${download.packageName}")

        // Notify for installation
        Intent(appContext, InstallReceiver::class.java).also {
            it.action = InstallReceiver.ACTION_INSTALL_APP
            it.putExtra(Constants.STRING_APP, download.packageName)
            it.putExtra(Constants.STRING_VERSION, download.versionCode)
            appContext.sendBroadcast(it)
        }
        return Result.success()
    }

    private suspend fun onFailure() {
        withContext(NonCancellable) {
            Log.i(TAG, "Cleaning up!")
            notifyStatus(DownloadStatus.FAILED)
            PathUtil.getAppDownloadDir(appContext, download.packageName, download.versionCode)
                .deleteRecursively()
            with(appContext.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager) {
                cancel(NOTIFICATION_ID)
            }
        }
    }

    private fun getDownloadRequest(files: List<GPlayFile>, libPackageName: String?): List<Request> {
        val downloadList = mutableListOf<Request>()
        files.filter { it.url.isNotBlank() }.forEach {
            val filePath = when (it.type) {
                GPlayFile.FileType.BASE, GPlayFile.FileType.SPLIT -> {
                    PathUtil.getApkDownloadFile(
                        appContext, download.packageName, download.versionCode, it, libPackageName
                    )
                }

                GPlayFile.FileType.OBB, GPlayFile.FileType.PATCH -> {
                    PathUtil.getObbDownloadFile(download.packageName, it)
                }
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

    private suspend fun onProgress(downloadInfo: DownloadInfo) {
        if (!isStopped && !download.isFinished) {
            val progress = ((downloadedBytes + downloadInfo.bytesCopied) * 100 / totalBytes).toInt()

            // Individual file progress can be negligible in contrast to total progress
            // Only notify the UI if progress is greater to avoid being rate-limited by Android
            if (progress > totalProgress) {
                val bytesRemaining = totalBytes - (downloadedBytes + downloadInfo.bytesCopied)
                val speed = if (downloadInfo.speed == 0L) 1 else downloadInfo.speed

                if (downloadInfo.progress == 100) {
                    downloadedBytes += downloadInfo.bytesCopied
                }

                download.apply {
                    this.status = DownloadStatus.DOWNLOADING
                    this.progress = progress
                    this.speed = downloadInfo.speed
                    this.timeRemaining = bytesRemaining / speed * 1000
                }
                downloadDao.update(download)

                notifyStatus(DownloadStatus.DOWNLOADING, NOTIFICATION_ID)
                totalProgress = progress
            }
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = if (this::download.isInitialized) {
            NotificationUtil.getDownloadNotification(appContext, download, id)
        } else {
            NotificationUtil.getDownloadNotification(appContext)
        }
        return if (isQAndAbove()) {
            ForegroundInfo(NOTIFICATION_ID, notification, FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private suspend fun notifyStatus(status: DownloadStatus, dID: Int = -1) {
        // Update database for all status except downloading which is handled onProgress
        if (status != DownloadStatus.DOWNLOADING) {
            download.apply {
                this.status = status
                if (download.status == DownloadStatus.COMPLETED) this.progress = 100
            }
            downloadDao.update(download)
        }

        val notification = NotificationUtil.getDownloadNotification(appContext, download, id)
        val notificationID = if (dID != -1) dID else download.packageName.hashCode()
        notificationManager.notify(notificationID, notification)
    }
}
