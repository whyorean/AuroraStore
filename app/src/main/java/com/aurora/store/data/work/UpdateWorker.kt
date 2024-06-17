package com.aurora.store.data.work

import android.app.NotificationManager
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
import com.aurora.extensions.isIgnoringBatteryOptimizations
import com.aurora.extensions.isMAndAbove
import com.aurora.extensions.isValid
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.util.AppUtil
import com.aurora.store.util.DownloadWorkerUtil
import com.aurora.store.util.NotificationUtil
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_AUTO
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_CHECK_INTERVAL
import com.aurora.store.util.save
import com.google.gson.Gson
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit.HOURS
import java.util.concurrent.TimeUnit.MINUTES

@HiltWorker
class UpdateWorker @AssistedInject constructor(
    private val downloadWorkerUtil: DownloadWorkerUtil,
    private val gson: Gson,
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "UpdateWorker"
        private const val UPDATE_WORKER = "UPDATE_WORKER"

        fun cancelAutomatedCheck(context: Context) {
            Log.i(TAG, "Cancelling periodic app updates!")
            WorkManager.getInstance(context).cancelUniqueWork(UPDATE_WORKER)
        }

        fun scheduleAutomatedCheck(context: Context) {
            Log.i(TAG,"Scheduling periodic app updates!")
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(UPDATE_WORKER, KEEP, buildUpdateWork(context))
        }

        fun updateAutomatedCheck(context: Context) {
            Log.i(TAG,"Updating periodic app updates!")
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

    private val notificationID = 100

    override suspend fun doWork(): Result {
        Log.i(TAG, "Checking for app updates")

        val autoUpdatesMode = Preferences.getInteger(appContext, PREFERENCE_UPDATES_AUTO, 3)
        val notifyManager =
            appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Exit if auto-updates is turned off in settings
        if (autoUpdatesMode == 0) {
            Log.i(TAG, "Auto-updates is disabled, bailing out!")
            return Result.failure()
        }

        withContext(Dispatchers.IO) {
            if (!AuthProvider.with(appContext).getAuthData().isValid(appContext)) {
                Log.i(TAG, "AuthData is not valid, retrying later!")
                return@withContext Result.retry()
            }

            try {
                val updatesList = AppUtil.getUpdatableApps(
                    context = appContext,
                    gson = gson,
                    verifyCert = true,
                    selfUpdate = false
                )

                if (updatesList.isNotEmpty()) {
                    if (autoUpdatesMode == 1) {
                        Log.i(TAG, "Found updates, notifying!")
                        notifyManager.notify(
                            notificationID,
                            NotificationUtil.getUpdateNotification(appContext, updatesList)
                        )
                    } else {
                        if (appContext.isIgnoringBatteryOptimizations()) {
                            Log.i(TAG, "Found updates, updating!")
                            updatesList.forEach { downloadWorkerUtil.enqueueApp(it) }
                        } else {
                            // Fallback to notification if battery optimizations are enabled
                            Log.i(TAG, "Found updates, but battery optimizations are enabled!")
                            notifyManager.notify(
                                notificationID,
                                NotificationUtil.getUpdateNotification(appContext, updatesList)
                            )
                            appContext.save(PREFERENCE_UPDATES_AUTO, 1)
                        }
                    }
                } else {
                    Log.i(TAG, "No updates found!")
                }
                return@withContext Result.success()
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch updates", exception)
                return@withContext Result.failure()
            }
        }
        return Result.success()
    }
}
