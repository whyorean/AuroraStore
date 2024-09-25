package com.aurora.store.data.work

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.content.getSystemService
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
import com.aurora.extensions.isOAndAbove
import com.aurora.extensions.isSAndAbove
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.data.installer.AppInstaller.Companion.Installer
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.util.AppUtil
import com.aurora.store.util.CertUtil
import com.aurora.store.util.DownloadWorkerUtil
import com.aurora.store.util.NotificationUtil
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_AUTO
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_CHECK_INTERVAL
import com.aurora.store.util.save
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit.HOURS
import java.util.concurrent.TimeUnit.MINUTES

/**
 * A periodic worker to check for updates for installed apps based on
 * filters and the auto-updates mode selected by the user. The repeat interval
 * is configurable by the user, defaulting to 3 hours with a flex time of 30 minutes.
 */
@HiltWorker
class UpdateWorker @AssistedInject constructor(
    private val appUtil: AppUtil,
    private val downloadWorkerUtil: DownloadWorkerUtil,
    private val authProvider: AuthProvider,
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "UpdateWorker"
        private const val UPDATE_WORKER = "UPDATE_WORKER"

        /**
         * Cancels the automated updates check
         * @param context Current [Context]
         * @see [UpdateWorker]
         */
        fun cancelAutomatedCheck(context: Context) {
            Log.i(TAG, "Cancelling periodic app updates!")
            WorkManager.getInstance(context).cancelUniqueWork(UPDATE_WORKER)
        }

        /**
         * Schedules the automated updates check
         * @param context Current [Context]
         * @see [UpdateWorker]
         */
        fun scheduleAutomatedCheck(context: Context) {
            Log.i(TAG,"Scheduling periodic app updates!")
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(UPDATE_WORKER, KEEP, buildUpdateWork(context))
        }

        /**
         * Updates the automated updates check to reconsider the new user preferences
         * @param context Current [Context]
         * @see [UpdateWorker]
         */
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
        val notificationManager = appContext.getSystemService<NotificationManager>()

        // Exit if auto-updates is turned off in settings
        if (autoUpdatesMode == 0) {
            Log.i(TAG, "Auto-updates is disabled, bailing out!")
            return Result.failure()
        }

        withContext(Dispatchers.IO) {
            if (!authProvider.isSavedAuthDataValid()) {
                Log.i(TAG, "AuthData is not valid, retrying later!")
                return@withContext Result.retry()
            }

            try {
                val updatesList = appUtil.checkUpdates()
                    .filter { it.hasValidCert }
                    .filterNot { it.isSelfUpdate() }

                if (updatesList.isNotEmpty()) {
                    if (autoUpdatesMode == 1) {
                        Log.i(TAG, "Found updates, notifying!")
                        notificationManager!!.notify(
                            notificationID,
                            NotificationUtil.getUpdateNotification(appContext, updatesList)
                        )
                    } else {
                        if (appContext.isIgnoringBatteryOptimizations()) {
                            // Trigger download for apps if they can be auto-updated (if any)
                            updatesList.filter { canAutoUpdate(it.packageName) }.let { list ->
                                if (list.isEmpty()) return@let

                                Log.i(TAG, "Found auto-update enabled apps, updating!")
                                list.forEach { downloadWorkerUtil.enqueueUpdate(it) }
                            }

                            // Notify about remaining apps (if any)
                            updatesList.filterNot { canAutoUpdate(it.packageName) }.let {  list ->
                                if (list.isEmpty()) return@let

                                Log.i(TAG, "Found apps that cannot be auto-updated, notifying!")
                                notificationManager!!.notify(
                                    notificationID,
                                    NotificationUtil.getUpdateNotification(appContext, list)
                                )
                            }
                        } else {
                            // Fallback to notification if battery optimizations are enabled
                            Log.i(TAG, "Found updates, but battery optimizations are enabled!")
                            notificationManager!!.notify(
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

    /**
     * Checks if the given package can be auto-updated or not
     */
    private fun canAutoUpdate(packageName: String): Boolean {
        return when (AppInstaller.getCurrentInstaller(appContext)) {
            Installer.SESSION -> isSAndAbove() && CertUtil.isAuroraStoreApp(appContext, packageName)
            Installer.NATIVE -> false
            Installer.ROOT -> AppInstaller.hasRootAccess()
            Installer.SERVICE -> AppInstaller.hasAuroraService(appContext)
            Installer.AM -> false // We cannot check if AppManager has ability to auto-update
            Installer.SHIZUKU -> isOAndAbove() && AppInstaller.hasShizukuOrSui(appContext) &&
                    AppInstaller.hasShizukuPerm()
        }
    }
}
