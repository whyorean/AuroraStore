package com.aurora.store.data.helper

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.aurora.gplayapi.data.models.App
import com.aurora.store.AuroraApp
import com.aurora.store.data.model.DownloadStatus
import com.aurora.store.data.room.download.Download
import com.aurora.store.data.room.download.DownloadDao
import com.aurora.store.data.room.suite.ExternalApk
import com.aurora.store.data.room.update.Update
import com.aurora.store.data.work.DownloadWorker
import com.aurora.store.util.PathUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Helper class to work with the [DownloadWorker].
 */
class DownloadHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadDao: DownloadDao
) {

    companion object {
        const val DOWNLOAD_WORKER = "DOWNLOAD_WORKER"
        const val PACKAGE_NAME = "PACKAGE_NAME"

        private const val DOWNLOAD_APP = "DOWNLOAD_APP"
        private const val DOWNLOAD_UPDATE = "DOWNLOAD_UPDATE"
        private const val VERSION_CODE = "VERSION_CODE"
    }

    val downloadsList get() = downloadDao.downloads()
        .stateIn(AuroraApp.scope, SharingStarted.WhileSubscribed(), emptyList())

    val pagedDownloads get() = downloadDao.pagedDownloads()

    private val TAG = DownloadHelper::class.java.simpleName

    /**
     * Removes failed download from the queue and starts observing for newly enqueued apps.
     */
    fun init() {
        AuroraApp.scope.launch {
            cancelFailedDownloads(downloadDao.downloads().firstOrNull() ?: emptyList())
        }.invokeOnCompletion {
            observeDownloads()
        }
    }

    private fun observeDownloads() {
        downloadDao.downloads().onEach { list ->
            try {
                if (list.none { it.status == DownloadStatus.DOWNLOADING }) {
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
        downloadDao.insert(Download.fromApp(app))
    }

    /**
     * Enqueues an update for download & install
     * @param update [Update] to download
     */
    suspend fun enqueueUpdate(update: Update) {
        downloadDao.insert(Download.fromUpdate(update))
    }

    /**
     * Enqueues ExternalApk for download & install
     * @param externalApk [ExternalApk] to download
     */
    suspend fun enqueueStandalone(externalApk: ExternalApk) {
        downloadDao.insert(Download.fromExternalApk(externalApk))
    }

    /**
     * Cancels the download for the given package
     * @param packageName Name of the package to cancel download
     */
    suspend fun cancelDownload(packageName: String) {
        Log.i(TAG, "Cancelling download for $packageName")
        WorkManager.getInstance(context).cancelAllWorkByTag("$PACKAGE_NAME:$packageName")
        downloadDao.updateStatus(packageName, DownloadStatus.CANCELLED)
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
        // Cancel all enqueued downloads first to avoid triggering re-download
        downloadDao.downloads().firstOrNull()
            ?.filter { it.status == DownloadStatus.QUEUED }
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

        val work = OneTimeWorkRequestBuilder<DownloadWorker>()
            .addTag(DOWNLOAD_WORKER)
            .addTag("$PACKAGE_NAME:${download.packageName}")
            .addTag("$VERSION_CODE:${download.versionCode}")
            .addTag(if (download.isInstalled) DOWNLOAD_UPDATE else DOWNLOAD_APP)
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
