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
import com.aurora.gplayapi.PlayClient
import com.aurora.gplayapi.PlayException
import com.aurora.gplayapi.android.AndroidLogger
import com.aurora.gplayapi.android.InstalledPatchBase
import com.aurora.gplayapi.model.AppFile
import com.aurora.gplayapi.model.PatchFormat
import com.aurora.gplayapi.net.HttpEngine
import com.aurora.gplayapi.session.PlaySessions
import com.aurora.store.AuroraApp
import com.aurora.store.R
import com.aurora.store.data.AccountRepository
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.data.helper.DownloadHelper
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.data.model.Algorithm
import com.aurora.store.data.model.DownloadInfo
import com.aurora.store.data.model.DownloadStatus
import com.aurora.store.data.model.DownloadStatus.Companion.installerStates
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.data.providers.GoogleAccountTokenProvider
import com.aurora.store.data.room.download.Download
import com.aurora.store.data.room.download.DownloadDao
import com.aurora.store.module.NoAuth
import com.aurora.store.util.BsPatch
import com.aurora.store.util.CertUtil
import com.aurora.store.util.FrostingVerifier
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
import okhttp3.Headers.Companion.toHeaders
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/**
 * An expedited long-running worker to download and trigger installation for given apps.
 *
 * Avoid using this worker directly and prefer using [DownloadHelper] instead.
 */
@HiltWorker
class DownloadWorker @AssistedInject constructor(
    private val authProvider: AuthProvider,
    tokenProvider: GoogleAccountTokenProvider,
    private val downloadDao: DownloadDao,
    private val appInstaller: AppInstaller,
    private val okHttpClient: OkHttpClient,
    private val httpEngine: HttpEngine,
    private val playClient: PlayClient,
    @NoAuth private val noAuthPlayClient: PlayClient,
    private val accountRepository: AccountRepository,
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters
) : AuthWorker(authProvider, tokenProvider, context, workerParams) {

    companion object {
        private const val NOTIFICATION_ID: Int = 200

        // Upper bound on automatic WorkManager retries for transient (network) failures
        // before the download is marked as failed and left for the user to retry.
        private const val MAX_DOWNLOAD_RETRIES = 5
    }

    private lateinit var download: Download
    private lateinit var deliveryClient: PlayClient

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

    // The installed copy this run asked Play for patches against, whose base APK hashes
    // planPatches reuses instead of hashing that file a second time.
    private var patchBase: InstalledPatchBase? = null

    inner class NoNetworkException : Exception(context.getString(R.string.title_no_network))
    inner class NothingToDownloadException : Exception(context.getString(R.string.purchase_no_file))
    inner class DownloadFailedException : Exception(context.getString(R.string.download_failed))
    inner class DownloadCancelledException :
        Exception(context.getString(R.string.download_canceled))

    inner class VerificationFailedException :
        Exception(context.getString(R.string.verification_failed))

    inner class ExpiredUrlException : Exception(context.getString(R.string.download_failed))

    inner class PatchTooLargeException : Exception("Patch exceeds its declared maximum size")

    inner class PatchVerificationException : Exception("Patched file failed verification")

    inner class InsufficientStorageException :
        Exception(context.getString(R.string.download_failed_storage))

    override suspend fun doWork(): Result {
        super.doWork()

        // Fetch required data for download
        try {
            download = downloadDao.getDownload(inputData.getString(DownloadHelper.PACKAGE_NAME)!!)
            deliveryClient = playClientFor(download.packageName)
        } catch (exception: Exception) {
            return onFailure(exception)
        }

        // Workers only ever start from a QUEUED row, so anything past it was committed by an
        // earlier run that never got to report success — as happens on a self-update, where
        // install() kills this process. Re-installing here would just kill the app again.
        if (download.status in installerStates) {
            Log.i(TAG, "${download.packageName} is already ${download.status}, nothing to do")
            return Result.success()
        }

        // The icon only decorates the progress notification. Fetching it is best-effort: a
        // failure (unreachable/pinned host, non-HTTP url, undecodable image) must never abort
        // the download itself.
        try {
            val response = get(download.iconURL).body
            val bitmap = BitmapFactory.decodeStream(
                withContext(Dispatchers.IO) { response.byteStream() }
            )
            icon = bitmap?.scale(96, 96)
        } catch (exception: Exception) {
            Log.w(TAG, "Failed to fetch icon for ${download.packageName}", exception)
        }

        // Set work/service to foreground on < Android 12.0
        setForeground(getForegroundInfo())

        // Resolve download URLs if the file list is empty. Surface any gplayapi error
        // (e.g. AppNotPurchased, AppNotSupported, AppRemoved) as the failure cause so
        // the user sees the real reason instead of a generic "files not available".
        notifyStatus(DownloadStatus.PURCHASING)
        try {
            download.fileList = download.fileList.ifEmpty {
                resolveFiles(download.packageName, download.versionCode, download.offerType)
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

        val files = mutableListOf<AppFile>()

        // A static shared library resolves by exact version and is never updated in place, so one
        // already installed at the required version is neither fetched nor handed to the
        // installer. Below API 26 the installed set cannot be read and every library is fetched.
        download = download.copy(
            sharedLibs = download.sharedLibs.filterNot {
                PackageUtil.isSharedLibraryInstalled(context, it.packageName, it.versionCode)
            }
        )

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

                    // Resolve the shared lib's own files if its list is empty
                    it.fileList = it.fileList.ifEmpty {
                        resolveFiles(it.packageName, it.versionCode, 0)
                    }
                    files.addAll(it.fileList)
                }
            } catch (exception: Exception) {
                return onFailure(exception)
            }
        }
        files.addAll(download.fileList)

        // Files Play patched against the installed copy are fetched as patches, which progress
        // counts by the patch's bytes rather than the whole file's.
        val patchSources = try {
            planPatches().toMutableMap()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Log.w(TAG, "Failed to plan patches for ${download.packageName}", exception)
            mutableMapOf()
        } catch (error: LinkageError) {
            Log.w(TAG, "Failed to plan patches for ${download.packageName}", error)
            mutableMapOf()
        }

        // Update data for notification
        download.totalFiles = files.size
        totalBytes = files.sumOf { if (it in patchSources) patchEstimate(it) else it.size }

        // Update database with all latest purchases
        downloadDao.updateFiles(download.packageName, download.fileList)
        downloadDao.updateSharedLibs(download.packageName, download.sharedLibs)

        // Fail fast (and let the system free its cache) if there isn't room for the download,
        // instead of dying mid-write with a partial file. Only the not-yet-downloaded bytes
        // need to fit.
        // A patched file needs room for its patch and for the file rebuilt from it.
        try {
            val onDisk = downloadedBytesOnDisk(files)
            val fullBytes = files.sumOf { it.size }
            try {
                ensureStorageAvailable(
                    fullBytes + patchSources.keys.sumOf { patchEstimate(it) } - onDisk
                )
            } catch (exception: InsufficientStorageException) {
                if (patchSources.isEmpty()) throw exception
                Log.i(TAG, "No room to apply patches for ${download.packageName} -> full download")
                patchSources.keys.forEach {
                    deletePatchFiles(PathUtil.getLocalFile(context, it, download))
                }
                patchSources.clear()
                totalBytes = fullBytes
                ensureStorageAvailable(fullBytes - downloadedBytesOnDisk(files))
            }
        } catch (exception: Exception) {
            return onFailure(exception)
        }

        // Download files
        try {
            for (file in files) {
                if (isStopped) {
                    throw DownloadCancelledException()
                }

                val installed = patchSources[file]
                if (installed == null || !downloadPatched(download.packageName, file, installed)) {
                    downloadFile(download.packageName, file)
                }
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
            files.filter { it.type == AppFile.Type.BASE }.forEach { file ->
                val result = withContext(Dispatchers.IO) {
                    FrostingVerifier.verify(PathUtil.getLocalFile(context, file, download))
                }
                Log.i(TAG, "Play frosting of ${download.packageName} @ ${file.name}: $result")
                require(result != FrostingVerifier.Result.INVALID)
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
                        val userMessage = when (exception) {
                            is PlayException.AppNotSupported -> context.getString(
                                R.string.download_version_unavailable,
                                download.versionCode
                            )

                            // Delivery is the only call this worker makes, and both session
                            // types carry Capability.DOWNLOAD, so the only way it can demand an
                            // account is the signing-key rotation the anonymous endpoint cannot
                            // express. The library's own message says so, in English only.
                            is PlayException.RequiresAccount -> context.getString(
                                R.string.download_signing_key_rotated
                            )

                            else -> exception.message?.takeIf { it.isNotBlank() }
                                ?: context.getString(R.string.download_failed)
                        }
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
     * The client this download resolves its files through.
     *
     * An app bound to a specific account gets a client pinned to that account's session:
     * Aurora records the account an app was installed with ([AccountRepository.bindApp]), and
     * a paid app bought on a second account has to be resolved as that account. Propagates a
     * refresh failure instead of falling back, which would acquire a bound app under an
     * identity the user did not pick.
     *
     * Everything else goes out on [noAuthPlayClient], with no account at all. A dispensed
     * account is shared by every Aurora user it was handed to, and its Play quota is consumed
     * by *downloads* far faster than by browsing - HTTP 429 on delivery is the failure users
     * actually hit. The account-less delivery endpoint has no such shared quota, so the
     * account's budget is spent only where an account is genuinely required. [resolveFiles]
     * falls back to the account when Play refuses an account-less resolve.
     *
     * Whether a session reaches the authenticated acquire/purchase/delivery flow or the
     * account-less delivery endpoint is gplayapi's call, made per request from the session's
     * own type.
     */
    private suspend fun playClientFor(packageName: String): PlayClient {
        val boundAccountId = accountRepository.boundAccountId(packageName)
            ?: return noAuthPlayClient
        val session = authProvider.sessionFor(boundAccountId)
        return PlayClient(httpEngine, PlaySessions { session }, AndroidLogger)
    }

    /**
     * Resolves the download URLs of the files needed to install [packageName] at
     * [versionCode], acquiring or purchasing it first if that is what the session's protocol
     * requires.
     *
     * Android 9.0+ supports signing-key rotation, so an update of an already-installed app
     * carries the installed certificate's hash; both delivery endpoints take it as `ch`.
     *
     * The installed-app check reads [download]'s package rather than [packageName], so a shared
     * library is asked for with the parent app's certificate hash. That is v3's behaviour kept
     * as found; nothing here can establish what Play does with a hash that belongs to a
     * different package.
     *
     * An update of [download]'s own app also describes the installed copy (`bvc`, `bda`, `bf`)
     * so Play can attach a patch to each file it can patch; a shared library never does, since
     * the installed copy described is the parent app's.
     */
    private suspend fun resolveFiles(
        packageName: String,
        versionCode: Long,
        offerType: Int
    ): List<AppFile> {
        val updatingInstalled =
            isPAndAbove && PackageUtil.isInstalled(context, download.packageName)
        val certificateHash = if (updatingInstalled) {
            CertUtil.getEncodedCertificateHashes(context, download.packageName).lastOrNull()
        } else {
            null
        }

        val base = if (packageName == download.packageName) readPatchBase() else null
        if (base != null) {
            patchBase = base
            Log.i(
                TAG,
                "Patch requested for $packageName against installed ${base.versionCode} " +
                    "(derived APK id ${if (base.derivedApkId != null) "present" else "absent"})"
            )
        }

        suspend fun resolveWith(client: PlayClient) = client.delivery.resolve(
            packageName = packageName,
            versionCode = versionCode,
            offerType = offerType,
            certificateHash = certificateHash,
            installedVersionCode = base?.versionCode,
            patchFormats = listOf(PatchFormat.GZIPPED_BSDIFF),
            installedDerivedApkId = base?.derivedApkId,
            installedFrostingId = base?.frostingId
        )

        // With no signed-in account the fallback below has nothing better to offer - its
        // client would resolve to the same account-less session - so don't spend a second
        // request re-asking Play the question it has already refused.
        if (deliveryClient !== noAuthPlayClient || authProvider.authData == null) {
            return resolveWith(deliveryClient)
        }

        // Two things the account-less endpoint cannot serve: a paid app, and anything Play
        // scopes to a library. Each is a refusal, not a transport error, and each is exactly
        // what the signed-in account is still here for - so retry once on it and pin the rest
        // of this download to it.
        return try {
            resolveWith(noAuthPlayClient)
        } catch (exception: PlayException) {
            when (exception) {
                is PlayException.RequiresAccount,
                is PlayException.AppNotPurchased,
                is PlayException.Unauthorized -> {
                    Log.i(TAG, "Account-less delivery refused $packageName, using the account")
                    deliveryClient = playClient
                    resolveWith(playClient)
                }
                else -> throw exception
            }
        }
    }

    /**
     * The installed copy of [download]'s app to ask Play for patches against, or `null` when this
     * is not an update of it: the app is not installed, is already at or past the target
     * version, or its signing certificate cannot be read. Hashes the whole installed base APK.
     */
    private suspend fun readPatchBase(): InstalledPatchBase? {
        val packageName = download.packageName
        if (!PackageUtil.isInstalled(context, packageName)) return null
        if (PackageUtil.isInstalled(context, packageName, download.versionCode)) return null
        if (CertUtil.getEncodedCertificateHashes(context, packageName).isEmpty()) return null
        return withContext(Dispatchers.IO) {
            try {
                InstalledPatchBase.read(context, packageName)
            } catch (exception: Exception) {
                Log.w(TAG, "Failed to read the installed copy of $packageName", exception)
                null
            }
        }
    }

    /**
     * Picks which of [download]'s files to fetch as a patch, keyed to the installed file each
     * patch applies to. A file qualifies only when Play served a `GZIPPED_BSDIFF` patch and the
     * installed counterpart's hash is the patch's base: SHA-256 when Play sends one, else SHA-1.
     */
    private suspend fun planPatches(): Map<AppFile, File> = withContext(Dispatchers.IO) {
        val plan = mutableMapOf<AppFile, File>()
        for (file in download.fileList) {
            val patch = file.patch ?: continue
            if (file.type != AppFile.Type.BASE && file.type != AppFile.Type.SPLIT) continue
            if (patch.format != PatchFormat.GZIPPED_BSDIFF.value || patch.url.isBlank()) {
                Log.i(TAG, "Patch for ${file.name} is format ${patch.format}, using full download")
                continue
            }

            val installed = installedCounterpart(file)
            if (installed == null) {
                Log.i(TAG, "Patch base missing for ${file.name} -> full download")
                continue
            }

            val matches = try {
                if (patch.baseSha256.isNotBlank()) {
                    patch.baseSha256.equals(installedHash(installed, Algorithm.SHA256), true)
                } else {
                    patch.baseSha1.isNotBlank() &&
                        patch.baseSha1.equals(installedHash(installed, Algorithm.SHA1), true)
                }
            } catch (exception: Exception) {
                Log.w(TAG, "Failed to hash the installed ${file.name}", exception)
                false
            }

            if (matches) {
                plan[file] = installed
            } else {
                Log.i(TAG, "Patch base mismatch for ${file.name} -> full download")
            }
        }
        plan
    }

    /**
     * The installed file [file]'s patch applies to: the base APK, or the installed split of the
     * same name. Split files are named `<split name>.apk`, and `PackageInfo.splitNames` lines up
     * with `ApplicationInfo.splitSourceDirs` index for index; `ApplicationInfo.splitNames` is
     * API 26, so it is not the one read.
     */
    private fun installedCounterpart(file: AppFile): File? {
        val packageInfo = try {
            PackageUtil.getPackageInfo(context, download.packageName)
        } catch (_: Exception) {
            null
        } ?: return null
        val applicationInfo = packageInfo.applicationInfo ?: return null

        val path = if (file.type == AppFile.Type.BASE) {
            applicationInfo.sourceDir
        } else {
            val splitName = file.name.removeSuffix(".apk")
            // Null, not empty, when the app has no splits.
            val splitNames: Array<String>? = packageInfo.splitNames
            val index = splitNames?.indexOf(splitName) ?: -1
            if (index >= 0) applicationInfo.splitSourceDirs?.getOrNull(index) else null
        }
        return path?.let { File(it) }?.takeIf { it.isFile }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun installedHash(file: File, algorithm: Algorithm): String {
        patchBase?.takeIf { it.baseApkPath == file.absolutePath }?.let {
            return if (algorithm == Algorithm.SHA256) it.baseApkSha256 else it.baseApkSha1
        }
        val digest = MessageDigest.getInstance(algorithm.value)
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().toHexString()
    }

    /** The bytes a patch is expected to take, for progress and storage before its size is known. */
    private fun patchEstimate(file: AppFile): Long {
        val maxSize = file.patch?.maxSize ?: 0L
        return if (maxSize > 0) minOf(maxSize, file.size) else file.size
    }

    /**
     * A plain GET on Aurora's own [OkHttpClient], used for the two transfers gplayapi has no
     * place for: fetching a notification icon, and streaming an APK off the delivery URL
     * gplayapi resolved. `HttpEngine` buffers a whole response body by design, so a multi-
     * hundred-megabyte split with resume, byte ranges and progress cannot go through it -
     * resolution and file transfer are separate concerns using separate tools on purpose.
     */
    private fun get(url: String, headers: Map<String, String> = emptyMap()): Response {
        val request = Request(url = url.toHttpUrl(), headers = headers.toHeaders())
        return okHttpClient.newCall(request).execute()
    }

    /**
     * Downloads the file from the given request.
     * Failed downloads aren't removed and persists as long as [CacheWorker] doesn't cleans them.
     * @param gFile An [AppFile] to download
     * @return A [Boolean] indicating whether the file was downloaded or not.
     */
    private suspend fun downloadFile(packageName: String, gFile: AppFile): Boolean {
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

            transfer(gFile.url, file, gFile.size)
            return@withContext true
        }
    }

    /**
     * Fetches [gFile] as its patch applied to [installed], and verifies the result against
     * [gFile]'s own hash before it takes the path a full download would use.
     *
     * Returns `false`, having removed everything it wrote, when the patch cannot be fetched,
     * applied or verified; the caller then downloads the whole file. Lost connectivity, a stop
     * and an expired URL propagate instead, since a full download would meet them too.
     */
    private suspend fun downloadPatched(
        packageName: String,
        gFile: AppFile,
        installed: File
    ): Boolean {
        val patch = gFile.patch ?: return false
        val file = PathUtil.getLocalFile(context, gFile, download)
        var accounted = patchEstimate(gFile)

        if (file.exists() && verifyFile(gFile)) {
            Log.i(TAG, "$file is already downloaded!")
            downloadedBytes += accounted
            verifiedFiles.add(file.absolutePath)
            return true
        }

        val patchFile = File(file.absolutePath + ".patch")
        val patchedFile = File(file.absolutePath + ".patched")
        val countedBefore = downloadedBytes
        Log.i(TAG, "Downloading $packageName @ ${gFile.name} as a patch")

        try {
            if (patchFile.exists() && (patch.maxSize <= 0 || patchFile.length() <= patch.maxSize)) {
                downloadedBytes += patchFile.length()
                totalBytes += patchFile.length() - accounted
                accounted = patchFile.length()
            } else {
                transfer(patch.url, patchFile, accounted, patch.maxSize) { length ->
                    totalBytes += length - accounted
                    accounted = length
                }
            }

            withContext(Dispatchers.IO) {
                BsPatch.applyFile(installed, patchFile, patchedFile, gFile.size) {
                    if (isStopped) throw CancellationException("Download stopped")
                }
            }

            if (!verifyFile(gFile, patchedFile)) throw PatchVerificationException()
            if (!patchedFile.renameTo(file)) throw IOException("Failed to rename $patchedFile")
            verifiedFiles.add(file.absolutePath)
            patchFile.delete()
            File(file.absolutePath + ".tmp").delete()
            Log.i(TAG, "Patch applied & verified for $packageName @ ${gFile.name}")
            return true
        } catch (exception: Exception) {
            patchedFile.delete()
            when (exception) {
                is NoNetworkException,
                is ExpiredUrlException,
                is DownloadCancelledException -> throw exception

                is CancellationException -> throw DownloadCancelledException()

                else -> {}
            }

            // A stop mid-apply must stay a stop, never turn into a full download.
            if (isStopped) throw DownloadCancelledException()

            // Exception messages are left out: a transport error can carry the patch's URL.
            val reason = if (exception is BsPatch.MalformedPatchException) {
                exception.message
            } else {
                exception.javaClass.simpleName
            }
            Log.i(TAG, "Patch failed for $packageName @ ${gFile.name} ($reason) -> full download")
            deletePatchFiles(file)
            file.delete()
            totalBytes += gFile.size - accounted + (downloadedBytes - countedBefore)
            return false
        }
    }

    /**
     * Streams [url] into [file] through `<file>.tmp`, resuming a partial `.tmp` when the server
     * honours the range, and counting every byte towards the download's progress.
     *
     * @param expectedSize the bytes [file] should come to, for per-file progress.
     * @param maxSize when positive, the transfer fails with [PatchTooLargeException] past it.
     * @param onLength called with the bytes [file] will come to, when the server says.
     */
    private suspend fun transfer(
        url: String,
        file: File,
        expectedSize: Long,
        maxSize: Long = 0L,
        onLength: (Long) -> Unit = {}
    ) {
        withContext(Dispatchers.IO) {
            try {
                // Download as a temporary file to avoid installing corrupted files
                val tmpFile = File(file.absolutePath + ".tmp")
                val existingBytes = if (tmpFile.exists()) tmpFile.length() else 0L

                val headers = mutableMapOf<String, String>()
                if (existingBytes > 0) {
                    Log.i(TAG, "$tmpFile has an unfinished download, requesting resume!")
                    headers["Range"] = "bytes=$existingBytes-"
                }

                val response = get(url, headers)
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

                var written = if (resuming) existingBytes else 0L
                val contentLength = response.body.contentLength()
                if (contentLength >= 0) onLength(written + contentLength)

                response.body.byteStream().use { input ->
                    FileOutputStream(tmpFile, resuming).use {
                        input.copyTo(it, expectedSize.coerceAtLeast(1L)).collect { info ->
                            // Abort promptly mid-file when stopped, instead of only checking
                            // between files (a single split can be hundreds of MB).
                            if (isStopped) throw CancellationException("Download stopped")
                            written += info.bytesCopied
                            if (maxSize > 0 && written > maxSize) {
                                throw PatchTooLargeException()
                            }
                            onProgress(info)
                        }
                    }
                }

                if (!tmpFile.renameTo(file)) {
                    throw Exception("Failed to remove .tmp extension from $tmpFile")
                }
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
     * Verifies integrity of a downloaded [AppFile].
     * @param gFile [AppFile] to verify
     * @param file the bytes to check against [gFile]'s hash, by default its download path
     */
    @OptIn(ExperimentalStdlibApi::class)
    private suspend fun verifyFile(
        gFile: AppFile,
        file: File = PathUtil.getLocalFile(context, gFile, download)
    ): Boolean {
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

    private fun deleteFile(file: AppFile) {
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

        deletePatchFiles(apkFile)
    }

    /** Removes the patch, its partial download and any half-applied output kept beside [file]. */
    private fun deletePatchFiles(file: File) {
        listOf(".patch", ".patch.tmp", ".patched").forEach { suffix ->
            val patchFile = File(file.absolutePath + suffix)
            if (patchFile.exists()) {
                patchFile.delete()
                Log.i(TAG, "Deleted patch file: $patchFile")
            }
        }
    }

    /**
     * Bytes already present on disk (final or partial .tmp, or a patch's) for [files], so the
     * storage check only requires room for what's still left to fetch.
     */
    private fun downloadedBytesOnDisk(files: List<AppFile>): Long = files.sumOf { gFile ->
        val file = PathUtil.getLocalFile(context, gFile, download)
        val tmpFile = File(file.absolutePath + ".tmp")
        val patchFile = File(file.absolutePath + ".patch")
        val patchTmpFile = File(file.absolutePath + ".patch.tmp")
        when {
            file.exists() -> file.length()
            tmpFile.exists() -> tmpFile.length()
            patchFile.exists() -> patchFile.length()
            patchTmpFile.exists() -> patchTmpFile.length()
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
