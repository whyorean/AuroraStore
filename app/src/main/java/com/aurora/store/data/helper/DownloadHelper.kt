package com.aurora.store.data.helper

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.aurora.extensions.TAG
import com.aurora.gplayapi.data.models.App
import com.aurora.store.AuroraApp
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.data.model.DownloadStatus
import com.aurora.store.data.room.download.Download
import com.aurora.store.data.room.download.DownloadDao
import com.aurora.store.data.room.suite.ExternalApk
import com.aurora.store.data.room.update.Update
import com.aurora.store.data.work.DownloadWorker
import com.aurora.store.util.PathUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Helper class to work with the [DownloadWorker].
 */
class DownloadHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadDao: DownloadDao,
    private val appInstaller: AppInstaller
) {

    companion object {
        const val DOWNLOAD_WORKER = "DOWNLOAD_WORKER"
        const val PACKAGE_NAME = "PACKAGE_NAME"

        private const val DOWNLOAD_APP = "DOWNLOAD_APP"
        private const val DOWNLOAD_UPDATE = "DOWNLOAD_UPDATE"
        private const val VERSION_CODE = "VERSION_CODE"
    }

    // Single stable StateFlow shared across consumers. Previously a `get()` accessor that
    // returned a new StateFlow on every read, which made `collectAsStateWithLifecycle` re-
    // collect on every recomposition and briefly replay the `emptyList()` initial value —
    // causing the Updates screen's per-item button to flicker between "Update" and "Cancel"
    // on every download tick.
    val downloadsList = downloadDao.downloads()
        .stateIn(AuroraApp.scope, SharingStarted.WhileSubscribed(), emptyList())

    val pagedDownloads get() = downloadDao.pagedDownloads()

    /**
     * One-shot read of the current download record for [packageName], if any.
     */
    suspend fun getDownload(packageName: String): Download? =
        downloadDao.downloads().first().find { it.packageName == packageName }

    /**
     * Removes failed download from the queue and starts observing for newly enqueued apps.
     */
    fun init() {
        AuroraApp.scope.launch {
            val downloads = downloadDao.downloads().firstOrNull() ?: emptyList()
            cancelFailedDownloads(downloads)
            finalizeStaleSelfUpdate(downloads)
        }.invokeOnCompletion {
            observeDownloads()
            observeInstalls()
        }
    }

    /**
     * Finalizes a self-update download left dangling in [DownloadStatus.INSTALLING]. Replacing
     * the app's own APK kills the process before the installer's "installed" event can advance
     * the row, so on the next launch it would otherwise show as installing forever. Reaching
     * INSTALLING means the install was already committed, so mark it installed; if it actually
     * failed, the periodic update check re-offers and re-enqueues it.
     */
    private suspend fun finalizeStaleSelfUpdate(downloads: List<Download>) {
        downloads.firstOrNull {
            it.packageName == context.packageName && it.status == DownloadStatus.INSTALLING
        }?.let {
            Log.i(TAG, "Finalizing stale self-update install for ${it.packageName}")
            downloadDao.updateStatus(it.packageName, DownloadStatus.INSTALLED)
        }
    }

    /**
     * Advances a download row through the installer phase so its history reflects whether the
     * app actually installed, not just that the bytes finished downloading:
     * - [InstallerEvent.Installing] moves a [DownloadStatus.COMPLETED] row to
     *   [DownloadStatus.INSTALLING];
     * - [InstallerEvent.Installed] marks it [DownloadStatus.INSTALLED] (kept so the user can
     *   still export the APK);
     * - [InstallerEvent.Failed] reverts an in-progress install back to
     *   [DownloadStatus.COMPLETED] so the downloaded files can be re-installed without
     *   re-downloading.
     */
    private fun observeInstalls() {
        AuroraApp.events.installerEvent.onEach { event ->
            val existing = getDownload(event.packageName) ?: return@onEach
            when (event) {
                is InstallerEvent.Installing -> if (existing.status == DownloadStatus.COMPLETED) {
                    downloadDao.updateStatus(event.packageName, DownloadStatus.INSTALLING)
                }

                is InstallerEvent.Installed -> if (existing.status != DownloadStatus.INSTALLED) {
                    downloadDao.updateStatus(event.packageName, DownloadStatus.INSTALLED)
                }

                is InstallerEvent.Failed -> if (existing.status == DownloadStatus.INSTALLING) {
                    downloadDao.updateStatus(event.packageName, DownloadStatus.COMPLETED)
                }

                else -> {}
            }
        }.launchIn(AuroraApp.scope)
    }

    private fun observeDownloads() {
        downloadDao.downloads().onEach { list ->
            try {
                // Serialize downloads: only start the next queued item once nothing else is
                // actively purchasing/downloading/verifying. Previously this only checked for
                // DOWNLOADING, so a worker in PURCHASING/VERIFYING didn't count and a second
                // download could start concurrently and clobber the shared notification.
                if (list.none { it.status in DownloadStatus.processing }) {
                    list.find { it.status == DownloadStatus.QUEUED }
                        ?.let { queuedDownload ->
                            Log.i(TAG, "Enqueued download worker for ${queuedDownload.packageName}")
                            trigger(queuedDownload)
                        }
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to enqueue download worker", exception)
            }
        }.launchIn(AuroraApp.scope)
    }

    /**
     * Enqueues an app for download & install
     * @param app [App] to download
     */
    suspend fun enqueueApp(app: App) {
        enqueue(Download.fromApp(app))
    }

    /**
     * Enqueues an update for download & install
     * @param update [Update] to download
     */
    suspend fun enqueueUpdate(update: Update) {
        enqueue(Download.fromUpdate(update))
    }

    /**
     * Enqueues ExternalApk for download & install
     * @param externalApk [ExternalApk] to download
     */
    suspend fun enqueueStandalone(externalApk: ExternalApk) {
        enqueue(Download.fromExternalApk(externalApk))
    }

    /**
     * Inserts a new download row, but only when a (re)download is actually needed. For an
     * existing record of the same version this:
     * - **installs without re-downloading** if the files are already downloaded & verified
     *   (e.g. the user missed the system install prompt, or the periodic update check runs
     *   again before a pending install completed); or
     * - **skips** entirely if the download is still active (queued/purchasing/downloading/
     *   verifying), so the periodic [UpdateWorker] and repeated user taps can't reset it back
     *   to [DownloadStatus.QUEUED] and re-download it.
     *
     * A genuinely newer version, or a previously failed/cancelled download whose files are
     * gone, falls through and is (re)enqueued.
     */
    private suspend fun enqueue(download: Download) {
        val existing = getDownload(download.packageName)
        if (existing != null && existing.versionCode == download.versionCode) {
            if (existing.canInstall(context)) {
                Log.i(TAG, "${download.packageName} already downloaded, installing directly")
                runCatching {
                    appInstaller.getPreferredInstaller(notifyOnFallback = true).install(existing)
                }.onFailure { Log.e(TAG, "Failed to install ${download.packageName}", it) }
                return
            }
            if (existing.isActive) {
                Log.i(
                    TAG,
                    "Skipping enqueue for ${download.packageName}; already ${existing.status}"
                )
                return
            }
        }
        downloadDao.insert(download)
    }

    /**
     * Re-queues a previously failed (or otherwise inactive) download so it runs again. The
     * worker resumes from any verified files on disk, so a retry after an install failure
     * re-installs without re-downloading. No-op if the download is already active.
     * @param packageName Name of the package to retry
     */
    suspend fun retryDownload(packageName: String) {
        val existing = getDownload(packageName) ?: return
        if (existing.isActive) {
            Log.i(TAG, "Skipping retry for $packageName; already ${existing.status}")
            return
        }
        Log.i(TAG, "Retrying download for $packageName")
        downloadDao.updateStatus(packageName, DownloadStatus.QUEUED)
    }

    /**
     * Cancels the download for the given package
     * @param packageName Name of the package to cancel download
     */
    suspend fun cancelDownload(packageName: String) {
        Log.i(TAG, "Cancelling download for $packageName")
        WorkManager.getInstance(context).cancelAllWorkByTag("$PACKAGE_NAME:$packageName")
        // Abandon any session already staged for install so we don't leak it.
        runCatching { appInstaller.getPreferredInstaller().cancelInstall(packageName) }
        downloadDao.updateStatus(packageName, DownloadStatus.CANCELLED)
    }

    /**
     * Removes the download record from the database without deleting downloaded files.
     * @param packageName Name of the package
     */
    suspend fun removeDownload(packageName: String) {
        downloadDao.delete(packageName)
    }

    /**
     * Clears the entry & downloaded files for the given package
     * @param packageName Name of the package of the app
     * @param versionCode Version of the package
     */
    suspend fun clearDownload(packageName: String, versionCode: Long) {
        Log.i(TAG, "Clearing downloads for $packageName ($versionCode)")
        downloadDao.delete(packageName)
        PathUtil.getAppDownloadDir(context, packageName, versionCode)
            .deleteRecursively()
    }

    /**
     * Clears all the downloads and their downloaded files
     */
    suspend fun clearAllDownloads() {
        Log.i(TAG, "Clearing all downloads!")
        downloadDao.deleteAll()
        PathUtil.getDownloadDirectory(context).deleteRecursively()
        PathUtil.getOldDownloadDirectories(context).forEach { it.deleteRecursively() }
    }

    /**
     * Clears finished downloads and their downloaded files
     */
    suspend fun clearFinishedDownloads() {
        downloadDao.downloads().firstOrNull()?.filter { it.isFinished }?.forEach {
            clearDownload(it.packageName, it.versionCode)
        }
    }

    /**
     * Cancels all the ongoing and queued downloads
     * @param updatesOnly Whether to cancel only updates, defaults to false
     */
    suspend fun cancelAll(updatesOnly: Boolean = false) {
        // Cancel queued/completed downloads first to avoid triggering re-download and to give
        // the user immediate feedback for items that already finished downloading. The actual
        // OS-level install for COMPLETED entries is wrapped in NonCancellable inside the
        // worker and cannot be aborted — if it succeeds, the update row is removed via
        // InstallerEvent.Installed; if it fails, the user can re-trigger an update.
        val cancellableStatuses = setOf(DownloadStatus.QUEUED, DownloadStatus.COMPLETED)
        downloadDao.downloads().firstOrNull()
            ?.filter { it.status in cancellableStatuses }
            ?.filter { if (updatesOnly) it.isInstalled else true }
            ?.forEach {
                downloadDao.updateStatus(it.packageName, DownloadStatus.CANCELLED)
            }

        WorkManager.getInstance(context)
            .cancelAllWorkByTag(if (updatesOnly) DOWNLOAD_UPDATE else DOWNLOAD_APP)
    }

    private suspend fun cancelFailedDownloads(downloadList: List<Download>) {
        val workManager = WorkManager.getInstance(context)

        downloadList.filter { it.isRunning }.forEach {
            workManager.getWorkInfosByTagFlow("$PACKAGE_NAME:${it.packageName}").firstOrNull()
                ?.all { workInfo -> workInfo.state.isFinished }
                ?.run { downloadDao.updateStatus(it.packageName, DownloadStatus.FAILED) }
        }
    }

    private fun trigger(download: Download) {
        val inputData = Data.Builder()
            .putString(PACKAGE_NAME, download.packageName)
            .build()

        // Require connectivity so the worker doesn't spin up (or keep running) without a
        // network, and back off exponentially so transient failures resume cleanly once the
        // connection returns instead of hammering the server.
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val work = OneTimeWorkRequestBuilder<DownloadWorker>()
            .addTag(DOWNLOAD_WORKER)
            .addTag("$PACKAGE_NAME:${download.packageName}")
            .addTag("$VERSION_CODE:${download.versionCode}")
            .addTag(if (download.isInstalled) DOWNLOAD_UPDATE else DOWNLOAD_APP)
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .setExpedited(OutOfQuotaPolicy.DROP_WORK_REQUEST)
            .setInputData(inputData)
            .build()

        // Ensure all app downloads are unique to preserve individual records
        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "$DOWNLOAD_WORKER/${download.packageName}/${download.versionCode}",
                ExistingWorkPolicy.KEEP,
                work
            )
    }
}
