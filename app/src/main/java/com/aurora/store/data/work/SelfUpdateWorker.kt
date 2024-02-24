package com.aurora.store.data.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy.KEEP
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aurora.Constants
import com.aurora.extensions.isMAndAbove
import com.aurora.store.BuildConfig
import com.aurora.store.data.model.SelfUpdate
import com.aurora.store.data.network.HttpClient
import com.aurora.store.util.CertUtil
import com.aurora.store.util.DownloadWorkerUtil
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_SELF_UPDATE
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_CHECK_INTERVAL
import com.google.gson.Gson
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit.HOURS
import java.util.concurrent.TimeUnit.MINUTES

@HiltWorker
class SelfUpdateWorker @AssistedInject constructor(
    private val downloadWorkerUtil: DownloadWorkerUtil,
    private val gson: Gson,
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "SelfUpdateWorker"
        private const val SELF_UPDATE_WORKER = "SELF_UPDATE_WORKER"

        fun cancelAutomatedCheck(context: Context) {
            Log.i(TAG, "Cancelling periodic self-updates!")
            WorkManager.getInstance(context).cancelUniqueWork(SELF_UPDATE_WORKER)
        }

        fun scheduleAutomatedCheck(context: Context) {
            Log.i(TAG,"Scheduling periodic self-updates!")
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(SELF_UPDATE_WORKER, KEEP, buildUpdateWork(context))
        }

        fun updateAutomatedCheck(context: Context) {
            Log.i(TAG,"Updating periodic self-updates!")
            WorkManager.getInstance(context).updateWork(buildUpdateWork(context))
        }

        private fun buildUpdateWork(context: Context): PeriodicWorkRequest {
            val updateCheckInterval = Preferences.getInteger(
                context,
                PREFERENCE_UPDATES_CHECK_INTERVAL,
                3
            ).toLong()

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresBatteryNotLow(true)

            if (isMAndAbove()) constraints.setRequiresDeviceIdle(true)

            return PeriodicWorkRequestBuilder<UpdateWorker>(
                repeatInterval = updateCheckInterval,
                repeatIntervalTimeUnit = HOURS,
                flexTimeInterval = 30,
                flexTimeIntervalUnit = MINUTES
            ).setConstraints(constraints.build()).build()
        }
    }

    private val TAG = SelfUpdateWorker::class.java.simpleName
    private val RELEASE = "release"

    override suspend fun doWork(): Result {
        @Suppress("KotlinConstantConditions") // False-positive for build type always not being release
        if (BuildConfig.BUILD_TYPE != RELEASE && Preferences.getBoolean(appContext, PREFERENCE_SELF_UPDATE)) {
            Log.e(TAG, "Either build type is not release or self-update check is disabled")
            return Result.failure()
        }

        try {
            val response = HttpClient.getPreferredClient(appContext)
                .get(Constants.UPDATE_URL, mapOf())
            val selfUpdate = gson.fromJson(String(response.responseBytes), SelfUpdate::class.java)

            if (selfUpdate.versionCode > BuildConfig.VERSION_CODE) {
                if (CertUtil.isFDroidApp(appContext, BuildConfig.APPLICATION_ID)) {
                    if (selfUpdate.fdroidBuild.isNotEmpty()) {
                        downloadWorkerUtil.enqueueApp(SelfUpdate.toApp(selfUpdate, appContext))
                    }
                } else if (selfUpdate.auroraBuild.isNotEmpty()) {
                    downloadWorkerUtil.enqueueApp(SelfUpdate.toApp(selfUpdate, appContext))
                } else {
                    Log.e(TAG, "Update file is missing!")
                    return Result.failure()
                }
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to check Aurora Store updates", exception)
            return Result.failure()
        }

        Log.i(TAG, "No updates found")
        return Result.success()
    }
}
