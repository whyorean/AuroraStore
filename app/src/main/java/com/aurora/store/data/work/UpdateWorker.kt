package com.aurora.store.data.work

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
import androidx.core.content.pm.PackageInfoCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy.KEEP
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aurora.Constants
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.gplayapi.helpers.AuthValidator
import com.aurora.store.MainActivity
import com.aurora.store.R
import com.aurora.store.data.network.HttpClient
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.data.providers.BlacklistProvider
import com.aurora.store.util.Log
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit.HOURS
import java.util.concurrent.TimeUnit.MINUTES

class UpdateWorker(private val appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val WORK_NAME_CHECK = "WORK_NAME_CHECK"

        fun cancelAutomatedCheck(context: Context) {
            Log.i("Cancelling periodic app updates check!")
            WorkManager.getInstance(context)
                .cancelUniqueWork(WORK_NAME_CHECK)
        }

        fun scheduleAutomatedCheck(context: Context) {
            Log.i("Scheduling periodic app updates check!")

            val updateCheckInterval = Preferences.getInteger(
                context,
                Preferences.PREFERENCE_UPDATES_CHECK_INTERVAL,
                3
            ).toLong()

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresDeviceIdle(true)
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<UpdateWorker>(
                repeatInterval = updateCheckInterval,
                repeatIntervalTimeUnit = HOURS,
                flexTimeInterval = 30,
                flexTimeIntervalUnit = MINUTES
            ).setConstraints(constraints).build()

            val workManager = WorkManager.getInstance(context)
            workManager.enqueueUniquePeriodicWork(WORK_NAME_CHECK, KEEP, workRequest)
        }
    }

    private val notificationID = 100
    private val workerID = 101
    private val gapps: MutableSet<String> = hashSetOf(
        "com.chrome.beta",
        "com.chrome.canary",
        "com.chrome.dev",
        "com.android.chrome",
        "com.niksoftware.snapseed",
        "com.google.toontastic"
    )

    private val authData = AuthProvider.with(appContext)
        .getAuthData()
    private val blackList = BlacklistProvider.with(appContext)
        .getBlackList()

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(workerID, getOngoingNotification())
    }

    override suspend fun doWork(): Result {
        withContext(Dispatchers.IO) {

            if (!isValid(authData)) {
                Log.i("AuthData is not valid, retrying later!")
                return@withContext Result.retry()
            }

            Log.i("Checking for app updates")

            val appDetailsHelper = AppDetailsHelper(authData)
                .using(HttpClient.getPreferredClient(appContext))

            val isGoogleFilterEnabled = Preferences.getBoolean(
                appContext,
                Preferences.PREFERENCE_FILTER_GOOGLE
            )
            val packageInfoMap = PackageUtil.getPackageInfoMap(appContext)

            packageInfoMap.keys.let { packages ->
                /*Filter black list*/
                var filtersPackages = packages.filter { !blackList.contains(it) }

                /*Filter google apps*/
                if (isGoogleFilterEnabled) {
                    filtersPackages = filtersPackages
                        .filter { !it.startsWith("com.google") }
                        .filter { !gapps.contains(it) }
                }

                val appList = appDetailsHelper.getAppByPackageName(filtersPackages)
                    .filter { it.displayName.isNotEmpty() }

                val updatesList = appList.filter {
                    val packageInfo = packageInfoMap[it.packageName]
                    if (packageInfo != null) {
                        it.versionCode.toLong() > PackageInfoCompat.getLongVersionCode(packageInfo)
                    } else {
                        false
                    }
                }

                if (updatesList.isNotEmpty()) {
                    Log.i("Found updates, notifying!")
                    val notifyManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE)
                            as NotificationManager
                    notifyManager.notify(notificationID, getUpdateNotification(updatesList))
                    return@withContext Result.success()
                }

                Log.i("No updates found!")
                return@withContext Result.success()
            }
        }
        return Result.success()
    }

    private fun isValid(authData: AuthData): Boolean {
        return try {
            AuthValidator(authData)
                .using(HttpClient.getPreferredClient(appContext))
                .isValid()
        } catch (e: Exception) {
            false
        }
    }

    private fun getUpdateNotification(updatesList: List<App>): Notification {
        val contentIntent = PendingIntent.getActivity(
            appContext,
            0,
            Intent(appContext, MainActivity::class.java).apply {
                action = Constants.NAVIGATION_UPDATES
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(appContext, Constants.NOTIFICATION_CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_updates)
            .setContentTitle(
                if (updatesList.size == 1)
                    appContext.getString(
                        R.string.notification_updates_available_1,
                        updatesList.size
                    )
                else
                    appContext.getString(
                        R.string.notification_updates_available,
                        updatesList.size
                    )
            )
            .setContentText(
                when (updatesList.size) {
                    1 -> {
                        appContext.getString(
                            R.string.notification_updates_available_desc_1,
                            updatesList[0].displayName
                        )
                    }

                    2 -> {
                        appContext.getString(
                            R.string.notification_updates_available_desc_2,
                            updatesList[0].displayName,
                            updatesList[1].displayName
                        )
                    }

                    3 -> {
                        appContext.getString(
                            R.string.notification_updates_available_desc_3,
                            updatesList[0].displayName,
                            updatesList[1].displayName,
                            updatesList[2].displayName
                        )
                    }

                    else -> {
                        appContext.getString(
                            R.string.notification_updates_available_desc_4,
                            updatesList[0].displayName,
                            updatesList[1].displayName,
                            updatesList[2].displayName,
                            updatesList.size - 3
                        )
                    }
                }
            )
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .build()
    }

    private fun getOngoingNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            appContext,
            0,
            Intent(appContext, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(appContext, Constants.NOTIFICATION_CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle(appContext.getString(R.string.checking_for_updates))
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(FOREGROUND_SERVICE_IMMEDIATE)
            .setProgress(100, 0, true)
            .setOngoing(true)
            .build()
    }
}
