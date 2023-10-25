package com.aurora.store.data.work

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy.REPLACE
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aurora.extensions.copyTo
import com.aurora.extensions.isQAndAbove
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.helpers.PurchaseHelper
import com.aurora.store.data.model.DownloadStatus
import com.aurora.store.data.model.Request
import com.aurora.store.data.network.HttpClient
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.util.NotificationUtil
import com.aurora.store.util.PathUtil
import com.google.gson.Gson
import java.io.File
import java.net.URL
import java.nio.file.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import com.aurora.gplayapi.data.models.File as GPlayFile

class DownloadWorker(private val appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    companion object {
        const val DOWNLOAD_DATA = "DOWNLOAD_DATA"
        const val DOWNLOAD_PROGRESS = "DOWNLOAD_PROGRESS"

        private const val TAG = "DownloadWorker"

        fun downloadApp(context: Context, app: App) {
            Log.i(TAG, "Downloading ${app.packageName}")

            val downloadData = Data.Builder()
                .putString(DOWNLOAD_DATA, Gson().toJson(app))
                .build()

            val work = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(downloadData)
                .addTag(app.versionCode.toString())
                .setExpedited(OutOfQuotaPolicy.DROP_WORK_REQUEST)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(app.packageName, REPLACE, work)
        }
    }

    private lateinit var app: App
    private lateinit var appDownloadDir: Path
    private var downloading = false

    private val TAG = DownloadWorker::class.java.simpleName
    private val notificationID = 200

    private val gson = Gson()

    override suspend fun doWork(): Result {
        // Purchase the app (free apps needs to be purchased too)
        val authData = AuthProvider.with(appContext).getAuthData()
        val purchaseHelper = PurchaseHelper(authData)
            .using(HttpClient.getPreferredClient(appContext))

        // Try to parse input data into a valid app
        withContext(Dispatchers.Default) {
            try {
                app = gson.fromJson(inputData.getString(DOWNLOAD_DATA), App::class.java)
                appDownloadDir = Path(PathUtil.getPackageDirectory(appContext, app.packageName))
            } catch (exception: Exception) {
                Log.e(TAG, "Failed parsing requested app!", exception)
                notifyStatus(DownloadStatus.FAILED)
                return@withContext Result.failure()
            }
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
        Path(appDownloadDir.absolutePathString(), app.versionCode.toString()).createDirectories()

        val requestList = getDownloadRequest(files)
        requestList.forEach { request ->
            downloading = true
            downloadFile(request)
            while (downloading && !isStopped) {
                delay(1000)
            }
        }

        // Mark download as completed
        notifyStatus(DownloadStatus.COMPLETED)
        Log.i(TAG, "Finished downloading ${app.packageName}")
        return Result.success()
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
                    downloading = false
                    return@withContext Result.failure()
                }
                downloading = false
                return@withContext Result.success()
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to download ${request.filePath}!", exception)
                requestFile.delete()
                notifyStatus(DownloadStatus.FAILED)
                downloading = false
                return@withContext Result.failure()
            }
        }
    }

    private suspend fun onProgress(progress: Int) {
        setProgress(Data.Builder().putInt(DOWNLOAD_PROGRESS, progress).build())
        notifyStatus(DownloadStatus.DOWNLOADING, progress, notificationID)
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
        val notificationManager =
            appContext.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
        val notification =
            NotificationUtil.getDownloadNotification(appContext, app, status, progress, id)
        notificationManager.notify(if (dID != -1) dID else app.packageName.hashCode(), notification)
    }
}
