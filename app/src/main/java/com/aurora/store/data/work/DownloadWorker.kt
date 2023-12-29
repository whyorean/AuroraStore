package com.aurora.store.data.work

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.aurora.extensions.copyTo
import com.aurora.extensions.isPAndAbove
import com.aurora.extensions.isQAndAbove
import com.aurora.extensions.requiresObbDir
import com.aurora.gplayapi.helpers.PurchaseHelper
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.data.model.DownloadInfo
import com.aurora.store.data.model.DownloadStatus
import com.aurora.store.data.model.Request
import com.aurora.store.data.network.HttpClient
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.data.room.download.Download
import com.aurora.store.data.room.download.DownloadDao
import com.aurora.store.util.CertUtil
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
import java.security.DigestInputStream
import java.security.MessageDigest
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
    private lateinit var icon: Bitmap
    private lateinit var purchaseHelper: PurchaseHelper

    private val NOTIFICATION_ID = 200

    private var downloading = false
    private var totalBytes by Delegates.notNull<Long>()
    private var totalProgress = 0
    private var downloadedBytes = 0L

    private val TAG = DownloadWorker::class.java.simpleName

    override suspend fun doWork(): Result {
        // Try to parse input data into a valid app
        try {
            val downloadData = inputData.getString(DownloadWorkerUtil.DOWNLOAD_DATA)
            download = gson.fromJson(downloadData, Download::class.java)

            val bitmap = BitmapFactory.decodeStream(URL(download.iconURL).openStream())
            icon = Bitmap.createScaledBitmap(bitmap, 96, 96, true)
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to parse download data", exception)
            return Result.failure()
        }

        // Set work/service to foreground on < Android 12.0
        setForeground(getForegroundInfo())

        // Purchase the app (free apps needs to be purchased too)
        val authData = AuthProvider.with(appContext).getAuthData()
        purchaseHelper = PurchaseHelper(authData)
            .using(HttpClient.getPreferredClient(appContext))

        notificationManager =
            appContext.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager

        // Bail out if file list is empty
        download.fileList = download.fileList.ifEmpty {
            purchase(download.packageName, download.versionCode, download.offerType)
        }
        if (download.fileList.isEmpty()) {
            Log.i(TAG, "Nothing to download!")
            notifyStatus(DownloadStatus.FAILED)
            return Result.failure()
        }

        // Create dirs & generate download request for files and shared libs (if any)
        PathUtil.getAppDownloadDir(appContext, download.packageName, download.versionCode).mkdirs()
        if (download.fileList.requiresObbDir()) {
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
                it.fileList = it.fileList.ifEmpty { purchase(it.packageName, it.versionCode, 0) }
                requestList.addAll(getDownloadRequest(it.fileList, it.packageName))
            }
        }
        requestList.addAll(getDownloadRequest(download.fileList, null))

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
        onSuccess()
        return Result.success()
    }

    private suspend fun onSuccess() {
        withContext(NonCancellable) {
            try {
                val downloadDir = PathUtil.getAppDownloadDir(
                    appContext,
                    download.packageName,
                    download.versionCode
                )
                AppInstaller.getInstance(appContext)
                    .getPreferredInstaller()
                    .install(
                        download.packageName,
                        downloadDir.listFiles()!!.filter { it.path.endsWith(".apk") }
                    )
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to install ${download.packageName}", exception)
            }
        }
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

    private fun purchase(packageName: String, versionCode: Int, offerType: Int): List<GPlayFile> {
        // Android 9.0+ supports key rotation, so purchase with latest certificate's hash
        return if (isPAndAbove() && download.isInstalled) {
            purchaseHelper.purchase(
                packageName,
                versionCode,
                offerType,
                CertUtil.getEncodedCertificateHashes(appContext, download.packageName).last()
            )
        } else {
            purchaseHelper.purchase(packageName, versionCode, offerType)
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
            downloadList.add(Request(it.url, filePath, it.size, it.sha1, it.sha256))
        }
        return downloadList
    }

    @OptIn(ExperimentalStdlibApi::class)
    private suspend fun downloadFile(request: Request): Result {
        return withContext(Dispatchers.IO) {
            val requestFile = File(request.filePath)

            // If file exists, no need to download again
            if (!shouldDownload(request)) {
                Log.i(TAG, "$requestFile already exists")
                return@withContext Result.success()
            }

            try {
                val algorithm = if (request.sha256.isBlank()) "SHA-1" else "SHA-256"
                val messageDigest = MessageDigest.getInstance(algorithm)

                requestFile.createNewFile()
                DigestInputStream(URL(request.url).openStream(), messageDigest).use { input ->
                    requestFile.outputStream().use {
                        input.copyTo(it, request.size).collectLatest { p -> onProgress(p) }
                    }
                }

                val sha = messageDigest.digest().toHexString()
                if (!File(request.filePath).exists() || !(sha == request.sha1 || sha == request.sha256)) {
                    Log.e(TAG, "$requestFile is either missing or corrupt")
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
            NotificationUtil.getDownloadNotification(appContext, download, id, icon)
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

        val notification = NotificationUtil.getDownloadNotification(appContext, download, id, icon)
        val notificationID = if (dID != -1) dID else download.packageName.hashCode()
        notificationManager.notify(notificationID, notification)
    }

    @OptIn(ExperimentalStdlibApi::class)
    private suspend fun shouldDownload(request: Request): Boolean {
        return withContext(Dispatchers.IO) {
            val file = File(request.filePath)
            if (file.exists()) {
                val algorithm = if (request.sha256.isBlank()) "SHA-1" else "SHA-256"
                val messageDigest = MessageDigest.getInstance(algorithm)
                DigestInputStream(file.inputStream(), messageDigest).use { input ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var read = input.read(buffer, 0, DEFAULT_BUFFER_SIZE)
                    while (read > -1) {
                        read = input.read(buffer, 0, DEFAULT_BUFFER_SIZE)
                    }
                }
                val sha = messageDigest.digest().toHexString()
                return@withContext !(sha == request.sha1 || sha == request.sha256)
            } else {
                return@withContext true
            }
        }
    }
}
