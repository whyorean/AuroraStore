package com.aurora.store.util

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.aurora.extensions.isIgnoringBatteryOptimizations
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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively

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
        // Run cleanup for last finished download and drop it database
        GlobalScope.launch {
            downloadDao.downloads()
                .collectLatest { list ->
                    // Check and trigger next download in queue, if any
                    if (!list.any { it.status == DownloadStatus.DOWNLOADING }) {
                        val enqueuedDownloads = list.filter { it.status == DownloadStatus.QUEUED }
                        enqueuedDownloads.firstOrNull()?.let {
                            try {
                                if (context.isIgnoringBatteryOptimizations() || CommonUtil.inForeground()) {
                                    Log.i(DOWNLOAD_WORKER, "Downloading ${it.packageName}")
                                    trigger(it)
                                } else {
                                    Log.i(TAG, "Not in foreground or ignoring battery optimization")
                                    cancel()
                                }
                            } catch (exception: Exception) {
                                Log.i(DOWNLOAD_WORKER, "Failed to download app", exception)
                                downloadDao.delete(it.packageName)
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
        downloadsList.value
            .find { it.packageName == packageName && it.status == DownloadStatus.QUEUED }
            ?.let { downloadDao.update(it.copy(status = DownloadStatus.CANCELLED)) }
    }

    @OptIn(ExperimentalPathApi::class)
    suspend fun clearDownload(packageName: String, versionCode: Int) {
        Log.i(TAG, "Clearing downloads for $packageName ($versionCode)")
        downloadDao.delete(packageName)
        PathUtil.getAppDownloadDir(context, packageName, versionCode)
            .deleteRecursively()
    }

    suspend fun clearFinishedDownloads() {
        downloadsList.value.filter { it.isFinished }.forEach {
            clearDownload(it.packageName, it.versionCode)
        }
    }

    suspend fun cancelAll(downloads: Boolean = true, updates: Boolean = true) {
        // Cancel all enqueued downloads first to avoid triggering re-download
        downloadsList.value.filter { it.status == DownloadStatus.QUEUED }.forEach {
            downloadDao.update(it.copy(status = DownloadStatus.CANCELLED))
        }

        val workManager = WorkManager.getInstance(context)
        if (downloads) workManager.cancelAllWorkByTag(DOWNLOAD_APP)
        if (updates) workManager.cancelAllWorkByTag(DOWNLOAD_UPDATE)
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
