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
                                Log.i(DOWNLOAD_WORKER, "Downloading ${it.packageName}")
                                trigger(it)
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

    fun cancelDownload(packageName: String) {
        WorkManager.getInstance(context).cancelAllWorkByTag("$PACKAGE_NAME:$packageName")
    }

    fun cancelAll(context: Context, downloads: Boolean = true, updates: Boolean = true) {
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
