/*
 * SPDX-FileCopyrightText: 2025 Aurora OSS
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.work

import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.content.getSystemService
import androidx.core.graphics.scale
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
import com.aurora.gplayapi.data.models.PlayFile
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
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.DigestInputStream
import java.security.MessageDigest
import kotlin.coroutines.cancellation.CancellationException
import kotlin.properties.Delegates

/**
 * An expedited long-running worker to download and trigger installation for given apps.
 *
 * Avoid using this worker directly and prefer using [DownloadHelper] instead.
 */
@HiltWorker
class DownloadWorker @AssistedInject constructor(
    authProvider: AuthProvider,
    private val downloadDao: DownloadDao,
    private val appInstaller: AppInstaller,
    private val httpClient: IHttpClient,
    private val purchaseHelper: PurchaseHelper,
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters
) : AuthWorker(authProvider, context, workerParams) {

    private lateinit var download: Download

    private val notificationManager = context.getSystemService<NotificationManager>()!!

    private var icon: Bitmap? = null
    private var totalBytes by Delegates.notNull<Long>()
    private var totalProgress = 0
    private var downloadedBytes = 0L

    private val TAG = DownloadWorker::class.java.simpleName
    private val NOTIFICATION_ID: Int = 200

    inner class NoNetworkException : Exception(context.getString(R.string.title_no_network))
    inner class NothingToDownloadException : Exception(context.getString(R.string.purchase_no_file))
    inner class DownloadFailedException : Exception(context.getString(R.string.download_failed))
    inner class DownloadCancelledException : Exception(context.getString(R.string.download_canceled))
    inner class VerificationFailedException : Exception(context.getString(R.string.verification_failed))

    override suspend fun doWork(): Result {
        super.doWork()

        // Fetch required data for download
        try {
            download = downloadDao.getDownload(inputData.getString(DownloadHelper.PACKAGE_NAME)!!)

            val response = (httpClient as HttpClient).call(download.iconURL).body
            if (response != null) {
                val bitmap =
                    BitmapFactory.decodeStream(withContext(Dispatchers.IO) { response.byteStream() })
                icon = bitmap.scale(96, 96)
            }
        } catch (exception: Exception) {
            return onFailure(exception)
        }

        // Set work/service to foreground on < Android 12.0
        setForeground(getForegroundInfo())

        // Try to purchase the app if file list is empty
        download.fileList = download.fileList.ifEmpty {
            purchase(download.packageName, download.versionCode, download.offerType)
        }

        // Bail out if file list is empty after purchase
        if (download.fileList.isEmpty()) return onFailure(NothingToDownloadException())

        // Create dirs & generate download request for files and shared libs (if any)
        PathUtil.getAppDownloadDir(context, download.packageName, download.versionCode).mkdirs()

        // Create OBB dir if required
        if (download.fileList.requiresObbDir()) {
            PathUtil.getObbDownloadDir(download.packageName).mkdirs()
        }

        val files = mutableListOf<PlayFile>()

        // Check if shared libs are present, if yes, handle them first
        if (download.sharedLibs.isNotEmpty()) {
            download.sharedLibs.forEach {
                // Create shared lib download dir
                PathUtil.getLibDownloadDir(
                    context,
                    download.packageName,
                    download.versionCode,
                    it.packageName
                ).mkdirs()

                // Purchase shared lib if file list is empty
                it.fileList = it.fileList.ifEmpty {
                    purchase(it.packageName, it.versionCode, 0)
                }
                files.addAll(it.fileList)
            }
        }
        files.addAll(download.fileList)

        // Update data for notification
        download.totalFiles = files.size
        totalBytes = files.sumOf { it.size }

        // Update database with all latest purchases
        downloadDao.updateFiles(download.packageName, download.fileList)
        downloadDao.updateSharedLibs(download.packageName, download.sharedLibs)

        // Download files
        try {
            for (file in files) {
                if (isStopped) {
                    throw DownloadCancelledException()
                }

                downloadFile(download.packageName, file)
                download.downloadedFiles++
            }
        } catch (exception: Exception) {
            if (exception is DownloadCancelledException) {
                Log.i(TAG, "Download cancelled for ${download.packageName}")
                // Try to delete all downloaded files
                runCatching { files.forEach { deleteFile(it) } }
            }

            return onFailure(exception)
        }

        // Report failure if download was stopped or failed
        if (isStopped) return onFailure(DownloadFailedException())

        // Verify downloaded files
        try {
            notifyStatus(DownloadStatus.VERIFYING)
            files.forEach { file -> require(verifyFile(file)) }
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to verify ${download.packageName}", exception)
            onFailure(VerificationFailedException())
        }

        Log.i(TAG, "Finished downloading & verifying ${download.packageName}")
        notifyStatus(DownloadStatus.COMPLETED)

        return onSuccess()
    }

    private suspend fun onSuccess(): Result {
        return withContext(NonCancellable) {
            return@withContext try {
                appInstaller.getPreferredInstaller().install(download)
                Result.success()
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to install ${download.packageName}", exception)
                onFailure(exception)
            }
        }
    }

    private suspend fun onFailure(exception: Exception): Result {
        return withContext(NonCancellable) {
            Log.i(TAG, "Job failed: ${download.packageName}", exception)

            val cancelReasons = listOf(STOP_REASON_USER, STOP_REASON_CANCELLED_BY_APP)
            if (isSAndAbove && stopReason in cancelReasons) {
                notifyStatus(DownloadStatus.CANCELLED)
            } else {
                when (exception) {
                    is DownloadCancelledException -> {
                        notifyStatus(DownloadStatus.CANCELLED)
                    }

                    else -> {
                        notifyStatus(DownloadStatus.FAILED)
                        AuroraApp.events.send(
                            InstallerEvent.Failed(
                                packageName = download.packageName,
                                error = exception.stackTraceToString(),
                                extra = exception.message
                                    ?: context.getString(R.string.download_failed)
                            )
                        )
                    }
                }
            }

            // Remove all notifications
            notificationManager.cancel(NOTIFICATION_ID)

            return@withContext Result.failure()
        }
    }

    /**
     * Purchases the app to get the download URL of the required files
     * @param packageName The packageName of the app
     * @param versionCode Required version of the app
     * @param offerType Offer type of the app (free/paid)
     * @return A list of purchased files
     */
    private fun purchase(packageName: String, versionCode: Long, offerType: Int): List<PlayFile> {
        try {
            // Android 9.0+ supports key rotation, so purchase with latest certificate's hash
            return if (isPAndAbove && download.isInstalled) {
                purchaseHelper.purchase(
                    packageName,
                    versionCode,
                    offerType,
                    CertUtil.getEncodedCertificateHashes(context, download.packageName).last()
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
     * @param gFile A [PlayFile] to download
     * @return A [Boolean] indicating whether the file was downloaded or not.
     */
    private suspend fun downloadFile(packageName: String, gFile: PlayFile): Boolean {
        return withContext(Dispatchers.IO) {
            Log.i(TAG, "Downloading $packageName @ ${gFile.name}")
            val file = PathUtil.getLocalFile(context, gFile, download)

            // If file exists and has integrity intact, no need to download again
            if (file.exists() && verifyFile(gFile)) {
                Log.i(TAG, "$file is already downloaded!")
                downloadedBytes += file.length()
                return@withContext true
            }

            try {
                val tmpFileSuffix = ".tmp"
                val tmpFile = File(file.absolutePath + tmpFileSuffix)

                // Download as a temporary file to avoid installing corrupted files
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
                        input.copyTo(it, gFile.size).collect { info -> onProgress(info) }
                    }
                }

                if (!tmpFile.renameTo(file)) {
                    throw Exception("Failed to remove .tmp extension from $tmpFile")
                }

                return@withContext true
            } catch (exception: Exception) {
                when (exception) {
                    is SocketException,
                    is SocketTimeoutException,
                    is UnknownHostException -> {
                        throw NoNetworkException()
                    }

                    is CancellationException -> {
                        throw DownloadCancelledException()
                    }

                    else -> throw exception
                }
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

            val progress = ((downloadedBytes * 100L) / totalBytes).toInt()
            val bytesRemaining = totalBytes - downloadedBytes
            val speed = if (downloadInfo.speed == 0L) 1L else downloadInfo.speed

            // Consider a 10% change in speed
            val speedChanged = if (download.speed > 0) {
                val speedDifference = kotlin.math.abs(download.speed - speed)
                (speedDifference * 100.0 / download.speed) >= 10
            } else {
                // If previous speed was zero, any change matters
                speed != download.speed
            }

            // Individual file progress can be negligible in contrast to total progress
            // Only notify the UI if progress/speed change considerably to avoid being rate-limited by Android
            if ((progress - totalProgress) >= 5 || speedChanged) {
                download.apply {
                    this.progress = progress
                    this.speed = speed
                    this.timeRemaining = (bytesRemaining / speed) * 1000
                }

                downloadDao.updateProgress(
                    download.packageName,
                    download.progress,
                    download.speed,
                    download.timeRemaining
                )

                notifyStatus(DownloadStatus.DOWNLOADING, true)
                totalProgress = progress
            }
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = if (this::download.isInitialized) {
            NotificationUtil.getDownloadNotification(context, download, icon)
        } else {
            NotificationUtil.getDownloadNotification(context)
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
     */
    private suspend fun notifyStatus(status: DownloadStatus, isProgress: Boolean = false) {
        // Update status in database
        download.downloadStatus = status
        downloadDao.updateStatus(download.packageName, status)

        when (status) {
            DownloadStatus.VERIFYING,
            DownloadStatus.CANCELLED -> return

            DownloadStatus.COMPLETED -> {
                // Mark progress as 100 manually to avoid race conditions
                download.progress = 100
                downloadDao.updateProgress(download.packageName, 100, 0, 0)
            }

            else -> {}
        }

        val notification = NotificationUtil.getDownloadNotification(context, download, icon)
        notificationManager.notify(
            if (isProgress) NOTIFICATION_ID else download.packageName.hashCode(),
            notification
        )
    }

    /**
     * Verifies integrity of a downloaded [PlayFile].
     * @param gFile [PlayFile] to verify
     */
    @OptIn(ExperimentalStdlibApi::class)
    private suspend fun verifyFile(gFile: PlayFile): Boolean {
        val file = PathUtil.getLocalFile(context, gFile, download)
        Log.i(TAG, "Verifying $file")

        val algorithm = if (gFile.sha256.isBlank()) Algorithm.SHA1 else Algorithm.SHA256
        val expectedSha = if (algorithm == Algorithm.SHA1) gFile.sha1 else gFile.sha256

        if (expectedSha.isBlank()) return false

        return withContext(Dispatchers.IO) {
            try {
                val messageDigest = MessageDigest.getInstance(algorithm.value)
                file.inputStream().use { fis ->
                    DigestInputStream(fis, messageDigest).use { dis ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (dis.read(buffer) != -1) { /* Just read, digest updates automatically */
                        }
                    }
                }

                messageDigest.digest().toHexString() == expectedSha
            } catch (e: Exception) {
                Log.e(TAG, "Failed to verify $file", e)
                false
            }
        }
    }

    private fun deleteFile(file: PlayFile) {
        val apkFile = PathUtil.getLocalFile(context, file, download)
        if (apkFile.exists()) {
            apkFile.delete()
            Log.i(TAG, "Deleted Apk: $apkFile")
        }

        val tmpFile = File(apkFile.absolutePath + ".tmp")
        if (tmpFile.exists()) {
            tmpFile.delete()
            Log.i(TAG, "Deleted Temp: $tmpFile")
        }
    }
}
