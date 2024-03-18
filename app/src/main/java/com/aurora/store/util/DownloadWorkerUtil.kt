package com.aurora.store.util

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.aurora.gplayapi.data.models.App
import com.aurora.store.data.model.DownloadStatus
import com.aurora.store.data.room.download.Download
import com.aurora.store.data.room.download.DownloadDao
import com.aurora.store.data.work.DownloadWorker
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(DelicateCoroutinesApi::class)
class DownloadWorkerUtil @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadDao: DownloadDao,
    private val gson: Gson
) {

    companion object {
        const val DOWNLOAD_WORKER = "DOWNLOAD_WORKER"
        const val DOWNLOAD_DATA = "DOWNLOAD_DATA"

        private const val DOWNLOAD_APP = "DOWNLOAD_APP"
        private const val DOWNLOAD_UPDATE = "DOWNLOAD_UPDATE"
        private const val PACKAGE_NAME = "PACKAGE_NAME"
        private const val VERSION_CODE = "VERSION_CODE"
    }

    val downloadsList = downloadDao.downloads()
        .stateIn(GlobalScope, SharingStarted.WhileSubscribed(), emptyList())

    private val TAG = DownloadWorkerUtil::class.java.simpleName

    fun init() {
        GlobalScope.launch {
            cancelFailedDownloads(downloadDao.downloads().firstOrNull() ?: emptyList())
        }.invokeOnCompletion {
            observeDownloads()
        }
    }

    private fun observeDownloads() {
        GlobalScope.launch {
            downloadDao.downloads().collectLatest { list ->
                // Check and trigger next download in queue, if any
                if (!list.any { it.downloadStatus == DownloadStatus.DOWNLOADING }) {
                    val enqueuedDownloads = list.filter { it.downloadStatus == DownloadStatus.QUEUED }
                    enqueuedDownloads.firstOrNull()?.let {
                        try {
                            Log.i(DOWNLOAD_WORKER, "Downloading ${it.packageName}")
                            trigger(it)
                        } catch (exception: Exception) {
                            Log.i(DOWNLOAD_WORKER, "Failed to download app", exception)
                            downloadDao.updateStatus(it.packageName, DownloadStatus.FAILED)
                        }
                    }
                }
            }
        }
    }

    suspend fun enqueueApp(app: App) {
        downloadDao.insert(Download.fromApp(app))
    }

    suspend fun cancelDownload(packageName: String) {
        Log.i(TAG, "Cancelling download for $packageName")
        WorkManager.getInstance(context).cancelAllWorkByTag("$PACKAGE_NAME:$packageName")
        downloadsList.filter { it.isNotEmpty() }.firstOrNull()
            ?.find { it.packageName == packageName }
            ?.let { downloadDao.updateStatus(packageName, DownloadStatus.CANCELLED) }
    }

    suspend fun clearDownload(packageName: String, versionCode: Int) {
        Log.i(TAG, "Clearing downloads for $packageName ($versionCode)")
        downloadDao.delete(packageName)
        PathUtil.getAppDownloadDir(context, packageName, versionCode)
            .deleteRecursively()
    }

    suspend fun clearAllDownloads() {
        Log.i(TAG, "Clearing all downloads!")
        downloadDao.deleteAll()
        PathUtil.getDownloadDirectory(context).deleteRecursively()
    }

    suspend fun clearFinishedDownloads() {
        downloadsList.value.filter { it.isFinished }.forEach {
            clearDownload(it.packageName, it.versionCode)
        }
    }

    suspend fun cancelAll(updatesOnly: Boolean = false) {
        // Cancel all enqueued downloads first to avoid triggering re-download
        downloadsList.value.filter { it.downloadStatus == DownloadStatus.QUEUED }
            .filter { if (updatesOnly) it.isInstalled else true }.forEach {
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
            .putString(DOWNLOAD_DATA, gson.toJson(download))
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
                "${DOWNLOAD_WORKER}/${download.packageName}",
                ExistingWorkPolicy.KEEP, work
            )
    }
}
