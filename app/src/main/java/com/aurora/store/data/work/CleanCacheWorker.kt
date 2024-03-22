package com.aurora.store.data.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy.KEEP
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aurora.store.util.PathUtil
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_DOWNLOADS_CACHE_TIME
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.util.Calendar
import java.util.concurrent.TimeUnit.HOURS
import java.util.concurrent.TimeUnit.MINUTES
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@HiltWorker
class CleanCacheWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "CleanCacheWorker"
        private const val CLEAN_CACHE_WORKER = "CLEAN_CACHE_WORKER"

        fun scheduleAutomatedCacheCleanup(context: Context) {
            val periodicWorkRequest = PeriodicWorkRequestBuilder<CleanCacheWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = HOURS,
                flexTimeInterval = 30,
                flexTimeIntervalUnit = MINUTES
            ).build()

            Log.i(TAG, "Scheduling periodic cache cleanup!")
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(CLEAN_CACHE_WORKER, KEEP, periodicWorkRequest)
        }
    }

    private val cacheDuration = Preferences.getInteger(appContext, PREFERENCE_DOWNLOADS_CACHE_TIME, 6)
        .toLong()
        .toDuration(DurationUnit.HOURS)

    override suspend fun doWork(): Result {
        Log.i(TAG, "Cleaning cache")

        PathUtil.getOldDownloadDirectories(appContext).forEach { downloadDir -> // Downloads
            Log.i(TAG, "Deleting old unused download directory: $downloadDir")
            downloadDir.deleteRecursively()
        }

        PathUtil.getDownloadDirectory(appContext).listFiles()?.forEach { download -> // com.example.app
            // Delete if the download directory is empty
            if (download.listFiles().isNullOrEmpty()) {
                Log.i(TAG, "Removing empty download directory for ${download.name}")
                download.deleteRecursively(); return@forEach
            }

            download.listFiles()!!.forEach { versionCode -> // 20240325
                if (versionCode.listFiles().isNullOrEmpty()) {
                    // Purge empty non-accessible directory
                    Log.i(TAG, "Removing empty directory for ${download.name}, ${versionCode.name}")
                    versionCode.deleteRecursively()
                } else {
                    versionCode.deleteIfOld()
                }
            }
        }

        return Result.success()
    }

    private fun File.deleteIfOld() {
        val elapsedTime = Calendar.getInstance().timeInMillis - this.lastModified()
        if (elapsedTime.toDuration(DurationUnit.HOURS) > cacheDuration) {
            Log.i(TAG, "Removing $this older than ${cacheDuration.inWholeHours} hours")
            this.deleteRecursively()
        }
    }
}
