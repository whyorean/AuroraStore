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
import android.os.storage.StorageManager
import android.util.Log
import androidx.core.content.getSystemService
import androidx.core.graphics.scale
import androidx.hilt.work.HiltWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkInfo.Companion.STOP_REASON_CANCELLED_BY_APP
import androidx.work.WorkInfo.Companion.STOP_REASON_USER
import androidx.work.WorkerParameters
import com.aurora.extensions.TAG
import com.aurora.extensions.copyTo
import com.aurora.extensions.isOAndAbove
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
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.PathUtil
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_NOTIFICATION_PROGRESS
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection.HTTP_FORBIDDEN
import java.net.HttpURLConnection.HTTP_GONE
import java.net.HttpURLConnection.HTTP_PARTIAL
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.DigestInputStream
import java.security.MessageDigest
import kotlin.coroutines.cancellation.CancellationException
import kotlin.properties.Delegates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

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

    companion object {
        private const val NOTIFICATION_ID: Int = 200

        // Upper bound on automatic WorkManager retries for transient (network) failures
        // before the download is marked as failed and left for the user to retry.
        private const val MAX_DOWNLOAD_RETRIES = 5
    }

    private lateinit var download: Download

    private val notificationManager = context.getSystemService<NotificationManager>()!!

    // When the user opts out of progress notifications, the mandatory foreground notification
    // is kept minimal and per-tick progress refreshes are skipped. Read live so toggling the
    // setting takes effect on the next download.
    private val showProgress: Boolean
        get() = Preferences.getBoolean(context, PREFERENCE_NOTIFICATION_PROGRESS, true)

    private var icon: Bitmap? = null
    private var totalBytes by Delegates.notNull<Long>()
    private var totalProgress = 0
    private var downloadedBytes = 0L

    // Absolute paths of files already verified during the download pass, so the final
    // verification gate doesn't hash large APKs a second time.
    private val verifiedFiles = mutableSetOf<String>()

    inner class NoNetworkException : Exception(context.getString(R.string.title_no_network))
    inner class NothingToDownloadException : Exception(context.getString(R.string.purchase_no_file))
    inner class DownloadFailedException : Exception(context.getString(R.string.download_failed))
    inner class DownloadCancelledException :
        Exception(context.getString(R.string.download_canceled))

    inner class VerificationFailedException :
        Exception(context.getString(R.string.verification_failed))

    inner class ExpiredUrlException : Exception(context.getString(R.string.download_failed))

    inner class InsufficientStorageException :
        Exception(context.getString(R.string.download_failed_storage))

    override suspend fun doWork(): Result {
        super.doWork()

        // Fetch required data for download
        try {
            download = downloadDao.getDownload(inputData.getString(DownloadHelper.PACKAGE_NAME)!!)
        } catch (exception: Exception) {
            return onFailure(exception)
        }

        // The icon only decorates the progress notification. Fetching it is best-effort: a
        // failure (unreachable/pinned host, non-HTTP url, undecodable image) must never abort
        // the download itself.
        try {
            val response = (httpClient as HttpClient).call(download.iconURL).body
            val bitmap = BitmapFactory.decodeStream(
                withContext(Dispatchers.IO) { response.byteStream() }
            )
            icon = bitmap?.scale(96, 96)
        } catch (exception: Exception) {
            Log.w(TAG, "Failed to fetch icon for ${download.packageName}", exception)
        }

        // Set work/service to foreground on < Android 12.0
        setForeground(getForegroundInfo())

        // Try to purchase the app if file list is empty. Surface any GPlayApi error
        // (e.g. AppNotPurchased, AppNotSupported, AppRemoved) as the failure cause so
        // the user sees the real reason instead of a generic "files not available".
        notifyStatus(DownloadStatus.PURCHASING)
        try {
            download.fileList = download.fileList.ifEmpty {
                purchase(download.packageName, download.versionCode, download.offerType)
            }
        } catch (exception: Exception) {
            return onFailure(exception)
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
            try {
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
            } catch (exception: Exception) {
                return onFailure(exception)
            }
        }
        files.addAll(download.fileList)

        // Update data for notification
        download.totalFiles = files.size
        totalBytes = files.sumOf { it.size }

        // Update database with all latest purchases
        downloadDao.updateFiles(download.packageName, download.fileList)
        downloadDao.updateSharedLibs(download.packageName, download.sharedLibs)

        // Fail fast (and let the system free its cache) if there isn't room for the download,
        // instead of dying mid-write with a partial file. Only the not-yet-downloaded bytes
        // need to fit.
        try {
            ensureStorageAvailable(totalBytes - downloadedBytesOnDisk(files))
        } catch (exception: Exception) {
            return onFailure(exception)
        }

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
            // Only purge partial files on a genuine user/app cancellation. A stop caused
            // by lost connectivity, quota or device-state must keep the partials so the
            // retry resumes instead of re-downloading from scratch (this was the source of
            // downloads appearing to "restart" on a flaky network).
            if (exception is DownloadCancelledException && isCancelledByUser()) {
                Log.i(TAG, "Download cancelled by user for ${download.packageName}")
                runCatching { files.forEach { deleteFile(it) } }
            }

            return onFailure(exception)
        }

        // A stop that isn't a user cancellation (e.g. connectivity constraint) should be
        // retried with the partials intact rather than treated as a hard failure.
        if (isStopped) return onFailure(DownloadCancelledException())

        // Verify downloaded files (skipping any already verified during the download pass)
        try {
            notifyStatus(DownloadStatus.VERIFYING)
            files.forEach { file ->
                val path = PathUtil.getLocalFile(context, file, download).absolutePath
                if (path !in verifiedFiles) require(verifyFile(file))
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to verify ${download.packageName}", exception)
            // Drop the corrupt files so the next attempt re-downloads them clean instead
            // of resuming from a poisoned offset.
            runCatching { files.forEach { deleteFile(it) } }
            return onFailure(VerificationFailedException())
        }

        Log.i(TAG, "Finished downloading & verifying ${download.packageName}")
        notifyStatus(DownloadStatus.COMPLETED)

        return onSuccess()
    }

    private suspend fun onSuccess(): Result {
        return withContext(NonCancellable) {
            return@withContext try {
                // Update the ongoing foreground notification to reflect the install phase,
                // so the user sees a clean "Downloading -> Installing" progression instead of
                // a stale download bar lingering at 100%.
                notifyStatus(DownloadStatus.INSTALLING, isProgress = true)
                appInstaller.getPreferredInstaller(notifyOnFallback = true).install(download)
                Result.success()
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to install ${download.packageName}", exception)
                onFailure(exception)
            }
        }
    }

    /**
     * Whether the current stop/cancellation was initiated by the user (or the app on the
     * user's behalf) rather than by the system (connectivity/quota/device-state). Uses the
     * S+ [stopReason] when available and otherwise falls back to the persisted status, which
     * [DownloadHelper.cancelDownload] sets to [DownloadStatus.CANCELLED] before cancelling
     * the work — making this reliable below Android 12 too.
     */
    private suspend fun isCancelledByUser(): Boolean {
        val cancelReasons = listOf(STOP_REASON_USER, STOP_REASON_CANCELLED_BY_APP)
        if (isSAndAbove && stopReason in cancelReasons) return true
        return runCatching {
            downloadDao.getDownload(download.packageName).status == DownloadStatus.CANCELLED
        }.getOrDefault(false)
    }

    /**
     * Transient errors worth retrying once connectivity returns. Walks the cause chain so a
     * wrapped network error is still recognised.
     */
    private fun isRetryable(throwable: Throwable?): Boolean = when (throwable) {
        null -> false
        is NoNetworkException,
        is SocketException,
        is SocketTimeoutException,
        is UnknownHostException,
        // Expired URLs were re-purchased by clearing the file list; retrying re-fetches them.
        is ExpiredUrlException -> true

        else -> isRetryable(throwable.cause)
    }

    private suspend fun onFailure(exception: Exception): Result {
        return withContext(NonCancellable) {
            Log.i(TAG, "Job failed: ${download.packageName}", exception)

            val cancelledByUser = isCancelledByUser()

            // Retry transient failures (lost connectivity, system-initiated stops) with
            // backoff, keeping any partial download for resume. The network constraint on
            // the work request means the retry only runs once connectivity is back.
            val isSystemStop = exception is DownloadCancelledException && !cancelledByUser
            if (!cancelledByUser &&
                (isRetryable(exception) || isSystemStop) &&
                runAttemptCount < MAX_DOWNLOAD_RETRIES
            ) {
                Log.w(
                    TAG,
                    "Transient failure for ${download.packageName}, " +
                        "retrying (attempt $runAttemptCount)"
                )
                return@withContext Result.retry()
            }

            if (cancelledByUser) {
                notifyStatus(DownloadStatus.CANCELLED)
            } else {
                when (exception) {
                    is DownloadCancelledException -> {
                        notifyStatus(DownloadStatus.CANCELLED)
                    }

                    else -> {
                        val userMessage = exception.message?.takeIf { it.isNotBlank() }
                            ?: context.getString(R.string.download_failed)
                        notifyStatus(
                            status = DownloadStatus.FAILED,
                            message = userMessage
                        )
                        AuroraApp.events.send(
                            InstallerEvent.Failed(
                                packageName = download.packageName,
                                error = userMessage,
                                extra = exception.stackTraceToString()
                            )
                        )
                    }
                }
            }

            // Remove all notifications
            notificationManager.cancel(NOTIFICATION_ID)

            return@withContext Result.success()
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
        // Android 9.0+ supports key rotation, so purchase with latest certificate's hash
        return if (isPAndAbove && PackageUtil.isInstalled(context, download.packageName)) {
            purchaseHelper.purchase(
                packageName,
                versionCode,
                offerType,
                CertUtil.getEncodedCertificateHashes(context, download.packageName).last()
            )
        } else {
            purchaseHelper.purchase(packageName, versionCode, offerType)
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
                verifiedFiles.add(file.absolutePath)
                return@withContext true
            }

            try {
                // Download as a temporary file to avoid installing corrupted files
                val tmpFile = File(file.absolutePath + ".tmp")
                val existingBytes = if (tmpFile.exists()) tmpFile.length() else 0L

                val okHttpClient = httpClient as HttpClient
                val headers = mutableMapOf<String, String>()
                if (existingBytes > 0) {
                    Log.i(TAG, "$tmpFile has an unfinished download, requesting resume!")
                    headers["Range"] = "bytes=$existingBytes-"
                }

                val response = okHttpClient.call(gFile.url, headers)
                if (!response.isSuccessful) {
                    val code = response.code
                    response.close()
                    // Play download URLs are short-lived; a 403/410 means ours expired while
                    // the download sat queued. Drop the stale file lists so the retry
                    // re-purchases fresh URLs instead of hammering the dead one.
                    if (code == HTTP_FORBIDDEN || code == HTTP_GONE) {
                        Log.w(TAG, "Download URL for ${download.packageName} expired (code=$code)")
                        downloadDao.updateFiles(download.packageName, emptyList())
                        downloadDao.updateSharedLibs(
                            download.packageName,
                            download.sharedLibs.map { it.copy(fileList = emptyList()) }
                        )
                        throw ExpiredUrlException()
                    }
                    throw DownloadFailedException()
                }

                // Only resume when the server actually honored the Range request (206). If
                // it replied 200 with the full body we must overwrite from the start,
                // otherwise the full payload would be appended onto the existing partial and
                // silently corrupt the file.
                val resuming = existingBytes > 0 && response.code == HTTP_PARTIAL
                if (resuming) {
                    downloadedBytes += existingBytes
                } else if (existingBytes > 0) {
                    Log.w(
                        TAG,
                        "Server ignored Range for $tmpFile (code=${response.code}), restarting"
                    )
                }

                response.body.byteStream().use { input ->
                    FileOutputStream(tmpFile, resuming).use {
                        input.copyTo(it, gFile.size).collect { info ->
                            // Abort promptly mid-file when stopped, instead of only checking
                            // between files (a single split can be hundreds of MB).
                            if (isStopped) throw CancellationException("Download stopped")
                            onProgress(info)
                        }
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
        val notification = if (this::download.isInitialized && showProgress) {
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
    private suspend fun notifyStatus(
        status: DownloadStatus,
        isProgress: Boolean = false,
        message: String? = null
    ) {
        // Update status in database
        download.status = status
        downloadDao.updateStatus(download.packageName, status)

        when (status) {
            // Internal phases the user doesn't need a separate notification for: the ongoing
            // foreground progress notification already conveys that work is in progress.
            // Clear any stale per-app notification (e.g. a prior failure being retried) so it
            // doesn't linger.
            DownloadStatus.PURCHASING,
            DownloadStatus.VERIFYING,
            DownloadStatus.CANCELLED -> {
                notificationManager.cancel(download.packageName.hashCode())
                return
            }

            DownloadStatus.COMPLETED -> {
                // Mark progress as 100 manually to avoid race conditions
                download.progress = 100
                downloadDao.updateProgress(download.packageName, 100, 0, 0)

                // Silently-installable apps install automatically and get a single
                // "installed" notification afterwards, so a separate "download complete"
                // notice is just noise. Only surface completion when the user must act on it
                // (tap to install).
                val needsUserAction = !AppInstaller.canInstallSilently(
                    context,
                    download.packageName,
                    download.targetSdk
                )
                if (!needsUserAction) {
                    notificationManager.cancel(download.packageName.hashCode())
                    return
                }
            }

            else -> {}
        }

        // Skip detailed progress refreshes when the user has hidden progress; the minimal
        // foreground notification posted via getForegroundInfo keeps the download alive.
        if (isProgress && !showProgress) return

        val notification = NotificationUtil.getDownloadNotification(
            context,
            download,
            icon,
            message
        )
        notificationManager.notify(
            if (isProgress) NOTIFICATION_ID else download.packageName.hashCode(),
            notification
        )

        // A failed download is a grouped child; reconcile the failure summary so a bulk
        // update collapses into a single "N apps failed" entry.
        if (status == DownloadStatus.FAILED) {
            NotificationUtil.refreshGroupSummaries(context)
        }
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

        if (algorithm == Algorithm.SHA1) {
            Log.w(TAG, "No SHA-256 for ${gFile.name}, falling back to SHA-1")
        }

        if (expectedSha.isBlank()) return false

        return withContext(Dispatchers.IO) {
            try {
                val messageDigest = MessageDigest.getInstance(algorithm.value)
                file.inputStream().use { fis ->
                    DigestInputStream(fis, messageDigest).use { dis ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (dis.read(buffer) !=
                            -1
                        ) {
                            /* Just read, digest updates automatically */
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

    /**
     * Bytes already present on disk (final or partial .tmp) for [files], so the storage check
     * only requires room for what's still left to fetch.
     */
    private fun downloadedBytesOnDisk(files: List<PlayFile>): Long = files.sumOf { gFile ->
        val file = PathUtil.getLocalFile(context, gFile, download)
        val tmpFile = File(file.absolutePath + ".tmp")
        when {
            file.exists() -> file.length()
            tmpFile.exists() -> tmpFile.length()
            else -> 0L
        }
    }

    /**
     * Ensures there's room for [requiredBytes] before downloading, throwing
     * [InsufficientStorageException] otherwise. On Android O+ this also asks the system to
     * evict its own cache to make space, per the storage guidelines.
     */
    private fun ensureStorageAvailable(requiredBytes: Long) {
        if (requiredBytes <= 0) return

        val dir = PathUtil.getDownloadDirectory(context).apply { mkdirs() }
        if (isOAndAbove) {
            val storageManager = context.getSystemService<StorageManager>()!!
            try {
                val uuid = storageManager.getUuidForPath(dir)
                if (storageManager.getAllocatableBytes(uuid) < requiredBytes) {
                    throw InsufficientStorageException()
                }
                storageManager.allocateBytes(uuid, requiredBytes)
            } catch (exception: IOException) {
                Log.e(TAG, "Failed to allocate space for ${download.packageName}", exception)
                throw InsufficientStorageException()
            }
        } else if (dir.usableSpace < requiredBytes) {
            throw InsufficientStorageException()
        }
    }
}
