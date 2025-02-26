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
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.DigestInputStream
import java.security.MessageDigest
import kotlin.coroutines.cancellation.CancellationException
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
    private val appInstaller: AppInstaller,
    private val authProvider: AuthProvider,
    private val httpClient: IHttpClient,
    private val purchaseHelper: PurchaseHelper,
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters
) : AuthWorker(authProvider, appContext, workerParams) {

    private lateinit var download: Download

    private val notificationManager = appContext.getSystemService<NotificationManager>()!!

    private var notificationId: Int = 200
    private var icon: Bitmap? = null
    private var totalBytes by Delegates.notNull<Long>()
    private var totalProgress = 0
    private var downloadedBytes = 0L

    private val TAG = DownloadWorker::class.java.simpleName

    object Exceptions {
        val InvalidAuthDataException = Exception("AuthData is invalid")
        val NoPackageNameException = Exception("No packagename provided")
        val NothingToDownloadException = Exception("Failed to purchase app")
        val DownloadFailedException = Exception("Download was failed or cancelled")
        val VerificationFailedException = Exception("Verification of downloaded files failed")
    }

    override suspend fun doWork(): Result {
        super.doWork()

        // Bail out immediately if authData is not valid
        if (!authProvider.isSavedAuthDataValid()) return onFailure(Exceptions.InvalidAuthDataException)

        // Fetch required data for download
        try {
            val packageName = inputData.getString(DownloadHelper.PACKAGE_NAME)

            // Bail out if no package name is provided
            if (packageName.isNullOrBlank()) return onFailure(Exceptions.NoPackageNameException)

            notificationId = packageName.hashCode()
            download = downloadDao.getDownload(packageName)

            val response = (httpClient as HttpClient).call(download.iconURL).body
            if (response != null) {
                val bitmap =
                    BitmapFactory.decodeStream(withContext(Dispatchers.IO) { response.byteStream() })
                icon = Bitmap.createScaledBitmap(bitmap, 96, 96, true)
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
        if (download.fileList.isEmpty()) return onFailure(Exceptions.NothingToDownloadException)

        // Create dirs & generate download request for files and shared libs (if any)
        PathUtil.getAppDownloadDir(appContext, download.packageName, download.versionCode).mkdirs()

        // Create OBB dir if required
        if (download.fileList.requiresObbDir()) {
            PathUtil.getObbDownloadDir(download.packageName).mkdirs()
        }

        val files = mutableListOf<GPlayFile>()

        // Check if shared libs are present, if yes, handle them first
        if (download.sharedLibs.isNotEmpty()) {
            download.sharedLibs.forEach {
                // Create shared lib download dir
                PathUtil.getLibDownloadDir(
                    appContext,
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
        val downloadSuccess = downloadFiles(download.packageName, files)

        // Report failure if download was stopped or failed
        if (!downloadSuccess || isStopped) return onFailure(Exceptions.DownloadFailedException)

        // Verify downloaded files
        val verifySuccess = verifyFiles(download.packageName, files)

        // Report failure if verification failed
        if (!verifySuccess || isStopped) return onFailure(Exceptions.VerificationFailedException)

        Log.i(TAG, "Finished downloading & verifying ${download.packageName}")
        notifyStatus(DownloadStatus.COMPLETED)

        return onSuccess()
    }

    private suspend fun downloadFiles(
        packageName: String,
        files: List<GPlayFile>
    ): Boolean = withContext(Dispatchers.IO) {
        for (file in files) {
            try {
                downloadFile(file)

                if (isStopped) {
                    Log.w(TAG, "Download cancelled for $packageName")
                    return@withContext false
                }

                download.downloadedFiles++
            } catch (e: Exception) {
                Log.e(TAG, "Failed to download $packageName", e)
                return@withContext false
            }
        }

        true
    }

    private suspend fun verifyFiles(
        packageName: String,
        files: List<GPlayFile>
    ): Boolean = withContext(NonCancellable) {
        notifyStatus(DownloadStatus.VERIFYING)

        for (file in files) {
            try {
                verifyFile(file)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to verify $packageName : ${file.name}", e)
                return@withContext false
            }
        }

        true
    }

    private suspend fun onSuccess(): Result = withContext(NonCancellable) {
        return@withContext try {
            appInstaller.getPreferredInstaller().install(download)
            Result.success()
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to install ${download.packageName}", exception)
            onFailure(exception)
        }
    }

    private suspend fun onFailure(exception: Exception = Exception("Something went wrong!")): Result =
        withContext(NonCancellable) {
            Log.e(TAG, "Job failed: ${download.packageName}", exception)

            val cancelReasons = listOf(STOP_REASON_USER, STOP_REASON_CANCELLED_BY_APP)
            if (isSAndAbove && stopReason in cancelReasons) {
                notifyStatus(DownloadStatus.CANCELLED)
            } else {
                if (exception is CancellationException) {
                    notifyStatus(DownloadStatus.CANCELLED)
                } else {
                    notifyStatus(DownloadStatus.FAILED)
                    AuroraApp.events.send(InstallerEvent.Failed(download.packageName).apply {
                        extra = exception.message ?: appContext.getString(R.string.download_failed)
                        error = exception.stackTraceToString()
                    })
                }
            }

            // Remove all notifications
            notificationManager.cancel(notificationId)

            return@withContext Result.failure()
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
        Log.i(TAG, "Downloading ${gFile.name}")
        return withContext(Dispatchers.IO) {
            val file = PathUtil.getLocalFile(appContext, gFile, download)

            // If file exists and has integrity intact, no need to download again
            if (file.exists() && verifyFile(gFile)) {
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

                notifyStatus(DownloadStatus.DOWNLOADING)
                totalProgress = progress
            }
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = if (this::download.isInitialized) {
            NotificationUtil.getDownloadNotification(appContext, download, icon)
        } else {
            NotificationUtil.getDownloadNotification(appContext)
        }

        return if (isQAndAbove) {
            ForegroundInfo(notificationId, notification, FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    /**
     * Notifies the user of the current status of the download.
     * @param status Current [DownloadStatus]
     */
    private suspend fun notifyStatus(status: DownloadStatus) {
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

        val notification = NotificationUtil.getDownloadNotification(appContext, download, icon)
        notificationManager.notify(notificationId, notification)
    }

    /**
     * Verifies integrity of a downloaded [GPlayFile].
     * @param gFile [GPlayFile] to verify
     */
    @OptIn(ExperimentalStdlibApi::class)
    private suspend fun verifyFile(gFile: GPlayFile): Boolean {
        val file = PathUtil.getLocalFile(appContext, gFile, download)
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
}
