package com.aurora.store.data.work

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy.KEEP
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aurora.Constants
import com.aurora.extensions.isIgnoringBatteryOptimizations
import com.aurora.extensions.isMAndAbove
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.gplayapi.helpers.AuthValidator
import com.aurora.store.BuildConfig
import com.aurora.store.data.model.SelfUpdate
import com.aurora.store.data.network.HttpClient
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.data.providers.BlacklistProvider
import com.aurora.store.util.CertUtil
import com.aurora.store.util.DownloadWorkerUtil
import com.aurora.store.util.NotificationUtil
import com.aurora.store.util.PackageUtil
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
        private const val RELEASE = "release"

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

        suspend fun getSelfUpdate(context: Context, gson: Gson): App? {
            return withContext(Dispatchers.IO) {
                @Suppress("KotlinConstantConditions") // False-positive for build type always not being release
                if (BuildConfig.BUILD_TYPE != RELEASE) {
                    Log.i(TAG, "Self-updates are not available for this build!")
                    return@withContext null
                }

                try {
                    val response = HttpClient.getPreferredClient(context)
                        .get(Constants.UPDATE_URL, mapOf())
                    val selfUpdate =
                        gson.fromJson(String(response.responseBytes), SelfUpdate::class.java)

                    if (selfUpdate.versionCode > BuildConfig.VERSION_CODE) {
                        if (CertUtil.isFDroidApp(context, BuildConfig.APPLICATION_ID)) {
                            if (selfUpdate.fdroidBuild.isNotEmpty()) {
                                return@withContext SelfUpdate.toApp(selfUpdate, context)
                            }
                        } else if (selfUpdate.auroraBuild.isNotEmpty()) {
                            return@withContext SelfUpdate.toApp(selfUpdate, context)
                        } else {
                            Log.e(TAG, "Update file is missing!")
                            return@withContext null
                        }
                    }
                } catch (exception: Exception) {
                    Log.e(TAG, "Failed to check self-updates", exception)
                    return@withContext null
                }

                Log.i(TAG, "No self-updates found!")
                return@withContext null
            }
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

    private val updatesList = mutableListOf<App>()
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
        return ForegroundInfo(workerID, NotificationUtil.getOngoingUpdateNotification(appContext))
    }

    override suspend fun doWork(): Result {
        val autoUpdatesMode = Preferences.getInteger(appContext, PREFERENCE_UPDATES_AUTO, 3)
        val notifyManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Exit if auto-updates is turned off in settings
        if (autoUpdatesMode == 0) {
            Log.i(TAG,"Auto-updates is disabled, bailing out!")
            return Result.failure()
        }

        // Check for Aurora Store updates first
        if (!CertUtil.isFDroidApp(appContext, BuildConfig.APPLICATION_ID)) {
            getSelfUpdate(appContext, gson)?.let { updatesList.add(it) }
        }

        withContext(Dispatchers.IO) {

            if (!isValid(authData)) {
                Log.i(TAG,"AuthData is not valid, retrying later!")
                return@withContext Result.retry()
            }

            Log.i(TAG,"Checking for app updates")

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

                val appUpdatesList = appList.filter {
                    val packageInfo = packageInfoMap[it.packageName]
                    if (packageInfo != null) {
                        it.versionCode.toLong() > PackageInfoCompat.getLongVersionCode(packageInfo)
                    } else {
                        false
                    }
                }.filter { app ->
                    app.certificateSetList.any {
                        it.certificateSet in CertUtil.getEncodedCertificateHashes(
                            appContext,
                            app.packageName
                        )
                    }
                }
                updatesList.addAll(appUpdatesList)

                if (updatesList.isNotEmpty()) {
                    if (autoUpdatesMode == 1) {
                        Log.i(TAG,"Found updates, notifying!")
                        notifyManager.notify(
                            notificationID,
                            NotificationUtil.getUpdateNotification(appContext, updatesList)
                        )
                    } else {
                        if (appContext.isIgnoringBatteryOptimizations()) {
                            Log.i(TAG,"Found updates, updating!")
                            updatesList.forEach { downloadWorkerUtil.enqueueApp(it) }
                        } else {
                            // Fallback to notification if battery optimizations are enabled
                            Log.i(TAG,"Found updates, but battery optimizations are enabled!")
                            notifyManager.notify(
                                notificationID,
                                NotificationUtil.getUpdateNotification(appContext, updatesList)
                            )
                            appContext.save(PREFERENCE_UPDATES_AUTO, 1)
                        }
                    }
                    return@withContext Result.success()
                }

                Log.i(TAG,"No updates found!")
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
}
