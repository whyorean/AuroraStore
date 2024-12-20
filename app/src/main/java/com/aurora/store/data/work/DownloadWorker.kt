package com.aurora.store.data.work

import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.content.getSystemService
import androidx.hilt.work.HiltWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkInfo.Companion.STOP_REASON_CANCELLED_BY_APP
import androidx.work.WorkInfo.Companion.STOP_REASON_USER
import androidx.work.WorkerParameters
import com.aurora.extensions.copyTo
import com.aurora.extensions.isPAndAbove
import com.aurora.extensions.isQAndAbove
import com.aurora.extensions.isSAndAbove
import com.aurora.extensions.requiresObbDir
import com.aurora.gplayapi.helpers.PurchaseHelper
import com.aurora.gplayapi.network.IHttpClient
import com.aurora.store.AuroraApp
import com.aurora.store.R
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.data.helper.DownloadHelper
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.data.model.Algorithm
import com.aurora.store.data.model.DownloadInfo
import com.aurora.store.data.model.DownloadStatus
import com.aurora.store.data.network.HttpClient
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.data.room.download.Download
import com.aurora.store.data.room.download.DownloadDao
import com.aurora.store.util.CertUtil
import com.aurora.store.util.NotificationUtil
import com.aurora.store.util.PathUtil
import com.google.gson.Gson
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.DigestInputStream
import java.security.MessageDigest
import kotlin.properties.Delegates
import com.aurora.gplayapi.data.models.File as GPlayFile

/**
 * An expedited long-running worker to download and trigger installation for given apps.
 *
 * Avoid using this worker directly and prefer using [DownloadHelper] instead.
 */
@HiltWorker
class DownloadWorker @AssistedInject constructor(
    private val downloadDao: DownloadDao,
    private val gson: Gson,
    private val appInstaller: AppInstaller,
    private val authProvider: AuthProvider,
    private val httpClient: IHttpClient,
    private val purchaseHelper: PurchaseHelper,
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters
) : AuthWorker(authProvider, appContext, workerParams) {

    private lateinit var download: Download
    private lateinit var icon: Bitmap

    private val notificationManager = appContext.getSystemService<NotificationManager>()!!

    private val NOTIFICATION_ID = 200

    private var downloading = false
    private var totalBytes by Delegates.notNull<Long>()
    private var totalProgress = 0
    private var downloadedBytes = 0L

    private val TAG = DownloadWorker::class.java.simpleName

    override suspend fun doWork(): Result {
        super.doWork()

        // Bail out immediately if authData is not valid
        if (!authProvider.isSavedAuthDataValid()) {
            Log.e(TAG, "AuthData is not valid, exiting!")
            onFailure()
            return Result.failure()
        }

        // Try to parse input data into a valid app
        try {
            val downloadData = inputData.getString(DownloadHelper.DOWNLOAD_DATA)
            download = gson.fromJson(downloadData, Download::class.java)

            val bitmap = BitmapFactory.decodeStream(withContext(Dispatchers.IO) {
                (httpClient as HttpClient).call(download.iconURL).body!!.byteStream()
            })
            icon = Bitmap.createScaledBitmap(bitmap, 96, 96, true)
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to parse download data", exception)
            onFailure()
            return Result.failure()
        }

        // Set work/service to foreground on < Android 12.0
        setForeground(getForegroundInfo())

        // Bail out if file list is empty
        download.fileList = download.fileList.ifEmpty {
            purchase(download.packageName, download.versionCode, download.offerType)
        }
        if (download.fileList.isEmpty()) {
            Log.i(TAG, "Nothing to download!")
            onFailure(appContext.getString(R.string.purchase_failed))
            return Result.failure()
        }

        // Create dirs & generate download request for files and shared libs (if any)
        PathUtil.getAppDownloadDir(appContext, download.packageName, download.versionCode).mkdirs()
        if (download.fileList.requiresObbDir()) {
            PathUtil.getObbDownloadDir(download.packageName).mkdirs()
        }

        // Purchase the app (free apps needs to be purchased too)
        val requestList = mutableListOf<GPlayFile>()
        if (download.sharedLibs.isNotEmpty()) {
            download.sharedLibs.forEach {
                PathUtil.getLibDownloadDir(
                    appContext,
                    download.packageName,
                    download.versionCode,
                    it.packageName
                ).mkdirs()
                it.fileList = it.fileList.ifEmpty { purchase(it.packageName, it.versionCode, 0) }
                requestList.addAll(it.fileList)
            }
        }
        requestList.addAll(download.fileList)

        // Update data for notification
        download.totalFiles = requestList.size
        totalBytes = requestList.sumOf { it.size }

        // Update database with all latest purchases
        downloadDao.updateFiles(download.packageName, download.fileList)
        downloadDao.updateSharedLibs(download.packageName, download.sharedLibs)

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
                val d = downloadDao.getDownload(download.packageName)
                if (isStopped || d.downloadStatus == DownloadStatus.CANCELLED) {
                    onFailure()
                    break
                }
            }
        }

        // Mark download as completed
        onSuccess()
        return Result.success()
    }

    private suspend fun onSuccess() {
        withContext(NonCancellable) {
            Log.i(TAG, "Finished downloading ${download.packageName}")
            notifyStatus(DownloadStatus.COMPLETED)

            try {
                appInstaller.getPreferredInstaller().install(download)
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to install ${download.packageName}", exception)
            }
        }
    }

    private suspend fun onFailure(errorMessage: String? = null, exception: Exception? = null) {
        withContext(NonCancellable) {
            Log.i(TAG, "Failed downloading ${download.packageName}")
            val cancelReasons = listOf(STOP_REASON_USER, STOP_REASON_CANCELLED_BY_APP)
            if (isSAndAbove && stopReason in cancelReasons) {
                notifyStatus(DownloadStatus.CANCELLED)
            } else {
                notifyStatus(DownloadStatus.FAILED)
                AuroraApp.events.send(InstallerEvent.Failed(download.packageName).apply {
                    extra = errorMessage ?: appContext.getString(R.string.download_failed)
                    error = exception?.stackTraceToString() ?: String()
                })
            }

            notificationManager.cancel(NOTIFICATION_ID)
        }
    }

    /**
     * Purchases the app to get the download URL of the required files
     * @param packageName The packageName of the app
     * @param versionCode Required version of the app
     * @param offerType Offer type of the app (free/paid)
     * @return A list of purchased files
     */
    private fun purchase(packageName: String, versionCode: Int, offerType: Int): List<GPlayFile> {
        try {
            // Android 9.0+ supports key rotation, so purchase with latest certificate's hash
            return if (isPAndAbove && download.isInstalled) {
                purchaseHelper.purchase(
                    packageName,
                    versionCode,
                    offerType,
                    CertUtil.getEncodedCertificateHashes(appContext, download.packageName).last()
                )
            } else {
                purchaseHelper.purchase(packageName, versionCode, offerType)
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to purchase $packageName", exception)
            return emptyList()
        }
    }

    /**
     * Downloads the file from the given request.
     * Failed downloads aren't removed and persists as long as [CacheWorker] doesn't cleans them.
     * @param gFile A [GPlayFile] to download
     * @return A [Result] indicating whether the file was downloaded or not.
     */
    private suspend fun downloadFile(gFile: GPlayFile): Result {
        return withContext(Dispatchers.IO) {
            val file = PathUtil.getLocalFile(appContext, gFile, download)
            val algorithm = if (gFile.sha256.isBlank()) Algorithm.SHA1 else Algorithm.SHA256
            val expectedSha = if (algorithm == Algorithm.SHA1) gFile.sha1 else gFile.sha256

            // If file exists and sha matches the request, no need to download again
            if (file.exists() && validSha(file, expectedSha, algorithm)) {
                Log.i(TAG, "$file is already downloaded!")
                downloadedBytes += file.length()
                return@withContext Result.success()
            }

            try {
                // Download as a temporary file to avoid installing corrupted files
                val tmpFileSuffix = ".tmp"
                val tmpFile = File(file.absolutePath + tmpFileSuffix)
                val isNewFile = tmpFile.createNewFile()

                val okHttpClient = httpClient as HttpClient
                val headers = mutableMapOf<String, String>()

                if (!isNewFile) {
                    Log.i(TAG, "$tmpFile has an unfinished download, resuming!")
                    downloadedBytes += tmpFile.length()
                    headers["Range"] = "bytes=${tmpFile.length()}-"
                }

                okHttpClient.call(gFile.url, headers).body?.byteStream()?.use { input ->
                    FileOutputStream(tmpFile, !isNewFile).use {
                        input.copyTo(it, gFile.size).collect { p -> onProgress(p) }
                    }
                }

                // Ensure downloaded file matches expected sha
                if (!validSha(tmpFile, expectedSha, algorithm)) {
                    throw Exception("Incorrect hash for $tmpFile")
                }

                if (!tmpFile.renameTo(file)) {
                    throw Exception("Failed to remove .tmp extension from $tmpFile")
                }

                return@withContext Result.success()
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to download $file!", exception)
                notifyStatus(DownloadStatus.FAILED)
                return@withContext Result.failure()
            }
        }
    }

    /**
     * Updates the progress data of the download in the local database and notifies user.
     * @param downloadInfo An instance of [DownloadInfo]
     */
    private suspend fun onProgress(downloadInfo: DownloadInfo) {
        if (!isStopped && !download.isFinished) {
            downloadedBytes += downloadInfo.bytesCopied

            val progress = (downloadedBytes * 100 / totalBytes).toInt()
            val bytesRemaining = totalBytes - downloadedBytes
            val speed = if (downloadInfo.speed == 0L) 1 else downloadInfo.speed

            // Individual file progress can be negligible in contrast to total progress
            // Only notify the UI if progress is greater or speed has changed to avoid being rate-limited by Android
            if (progress > totalProgress || speed != download.speed) {
                download.apply {
                    this.progress = progress
                    this.speed = downloadInfo.speed
                    this.timeRemaining = bytesRemaining / speed * 1000
                }
                downloadDao.updateProgress(
                    download.packageName,
                    download.progress,
                    download.speed,
                    download.timeRemaining
                )

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

        return if (isQAndAbove) {
            ForegroundInfo(NOTIFICATION_ID, notification, FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    /**
     * Notifies the user of the current status of the download.
     * @param status Current [DownloadStatus]
     * @param dID ID of the notification, defaults to hashCode of the download's packageName
     */
    private suspend fun notifyStatus(status: DownloadStatus, dID: Int = -1) {
        // Update status in database
        download.downloadStatus = status
        downloadDao.updateStatus(download.packageName, status)

        when (status) {
            DownloadStatus.CANCELLED -> return
            DownloadStatus.COMPLETED -> {
                // Mark progress as 100 manually to avoid race conditions
                download.progress = 100
                downloadDao.updateProgress(download.packageName, 100, 0, 0)
            }

            else -> {}
        }

        val notification = NotificationUtil.getDownloadNotification(appContext, download, id, icon)
        val notificationID = if (dID != -1) dID else download.packageName.hashCode()
        notificationManager.notify(notificationID, notification)
    }

    /**
     * Validates whether given file has the expected SHA hash sum.
     * @param file [File] to check
     * @param expectedSha Expected SHA hash sum
     * @param algorithm [Algorithm] of the SHA
     * @return A boolean whether the given file has the expected SHA or not.
     */
    @OptIn(ExperimentalStdlibApi::class)
    private suspend fun validSha(file: File, expectedSha: String, algorithm: Algorithm): Boolean {
        return withContext(Dispatchers.IO) {
            val messageDigest = MessageDigest.getInstance(algorithm.value)
            DigestInputStream(file.inputStream(), messageDigest).use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var read = input.read(buffer, 0, DEFAULT_BUFFER_SIZE)
                while (read > -1) {
                    read = input.read(buffer, 0, DEFAULT_BUFFER_SIZE)
                }
            }
            val sha = messageDigest.digest().toHexString()
            return@withContext sha == expectedSha
        }
    }
}
