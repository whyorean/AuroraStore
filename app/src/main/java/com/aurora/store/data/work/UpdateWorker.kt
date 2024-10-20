package com.aurora.store.data.work

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.content.getSystemService
import androidx.core.content.pm.PackageInfoCompat
import androidx.hilt.work.HiltWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.aurora.Constants
import com.aurora.extensions.isIgnoringBatteryOptimizations
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.gplayapi.network.IHttpClient
import com.aurora.store.BuildConfig
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.data.helper.DownloadHelper
import com.aurora.store.data.helper.UpdateHelper
import com.aurora.store.data.model.BuildType
import com.aurora.store.data.model.SelfUpdate
import com.aurora.store.data.model.UpdateMode
import com.aurora.store.data.providers.AccountProvider
import com.aurora.store.data.providers.BlacklistProvider
import com.aurora.store.data.room.update.Update
import com.aurora.store.data.room.update.UpdateDao
import com.aurora.store.util.CertUtil
import com.aurora.store.util.NotificationUtil
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_AUTO
import com.google.gson.Gson
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A worker to check for updates for installed apps based on saved authentication data,
 * filters and the auto-updates mode selected by the user. The repeat interval
 * is configurable by the user, defaulting to 3 hours with a flex time of 30 minutes.
 *
 * Avoid using this worker directly and prefer using [UpdateHelper] instead.
 * @see AuthWorker
 */
@HiltWorker
class UpdateWorker @AssistedInject constructor(
    private val gson: Gson,
    private val blacklistProvider: BlacklistProvider,
    private val httpClient: IHttpClient,
    private val updateDao: UpdateDao,
    private val downloadHelper: DownloadHelper,
    private val authProvider: AuthProvider,
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters
) : AuthWorker(authProvider, appContext, workerParams) {

    private val TAG = UpdateWorker::class.java.simpleName

    private val notificationID = 100

    @Suppress("KotlinConstantConditions")
    private val buildType = when (BuildConfig.BUILD_TYPE) {
        "release" -> BuildType.RELEASE
        "nightly" -> BuildType.NIGHTLY
        else -> BuildType.DEBUG
    }

    private val canSelfUpdate = !CertUtil.isFDroidApp(appContext, BuildConfig.APPLICATION_ID) &&
            !CertUtil.isAppGalleryApp(appContext, BuildConfig.APPLICATION_ID) &&
            buildType != BuildType.DEBUG

    override suspend fun doWork(): Result {
        super.doWork()

        Log.i(TAG, "Checking for app updates")
        val updateMode = UpdateMode.entries[inputData.getInt(
            UpdateHelper.UPDATE_MODE,
            Preferences.getInteger(appContext, PREFERENCE_UPDATES_AUTO, 3)
        )]

        if (updateMode == UpdateMode.DISABLED || !AccountProvider.isLoggedIn(appContext)) {
            Log.i(TAG, "Updates are disabled, bailing out!")
            return Result.failure()
        }

        if (!authProvider.isSavedAuthDataValid()) {
            Log.i(TAG, "AuthData is not valid, retrying later!")
            return Result.retry()
        }

        try {
            val updates = checkUpdates()

            if (updates.isEmpty()) {
                Log.i(TAG, "No updates found!")
                return Result.success()
            }

            // Notify and exit if we are only checking for updates or if battery optimizations are enabled
            if (updateMode == UpdateMode.CHECK_ONLY || !appContext.isIgnoringBatteryOptimizations()) {
                Log.i(TAG, "Found updates, notifying!")
                notifyUpdates(updates)
                return Result.success()
            }

            // Clean the update list to prepare for installing
            val filteredUpdates = updates
                .filter { it.hasValidCert }
                .filterNot { it.isSelfUpdate(appContext) }
                .partition {
                    AppInstaller.canInstallSilently(appContext, it.packageName, it.targetSdk)
                }

            // Notify about apps that cannot be auto-updated
            notifyUpdates(filteredUpdates.second)

            // Trigger download for apps if they can be auto-updated
            filteredUpdates.first.forEach { downloadHelper.enqueueUpdate(it) }

            return Result.success()
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to fetch updates", exception)
            return Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            notificationID,
            NotificationUtil.getUpdateNotification(appContext)
        )
    }

    /**
     * Checks and returns updates for all possible apps
     */
    private suspend fun checkUpdates(): List<Update> {
        return withContext(Dispatchers.IO) {
            val packageInfoMap = PackageUtil.getPackageInfoMap(appContext)
            val appDetailsHelper = AppDetailsHelper(authProvider.authData!!)
                .using(httpClient)

            val updates = packageInfoMap.keys.let { packages ->
                val filteredPackages = packages.filter { !blacklistProvider.isBlacklisted(it) }
                appDetailsHelper.getAppByPackageName(filteredPackages)
                    .filter { it.displayName.isNotEmpty() }
                    .map { it.isInstalled = true; it }
            }.filter {
                val packageInfo = packageInfoMap[it.packageName]
                if (packageInfo != null) {
                    it.versionCode.toLong() > PackageInfoCompat.getLongVersionCode(packageInfo)
                } else {
                    false
                }
            }.toMutableList()

            if (canSelfUpdate) getSelfUpdate()?.let { updates.add(it) }

            return@withContext updates.map { Update.fromApp(appContext, it) }.also {
                // Cache the updates into the database
                updateDao.insertUpdates(it)
            }
        }
    }

    /**
     * Checks and returns updates for Aurora Store if available
     */
    private suspend fun getSelfUpdate(): App? {
        return withContext(Dispatchers.IO) {
            val updateUrl = when (buildType) {
                BuildType.RELEASE -> Constants.UPDATE_URL_STABLE
                BuildType.NIGHTLY -> Constants.UPDATE_URL_NIGHTLY
                else -> {
                    Log.i(TAG, "Self-updates are not available for this build!")
                    return@withContext null
                }
            }

            try {
                val response = httpClient.get(updateUrl, mapOf())
                val selfUpdate = gson.fromJson(
                    String(response.responseBytes),
                    SelfUpdate::class.java
                )

                val isUpdate = when (buildType) {
                    BuildType.RELEASE -> selfUpdate.versionCode > BuildConfig.VERSION_CODE
                    BuildType.NIGHTLY -> selfUpdate.timestamp > BuildConfig.TIMESTAMP
                    else -> false
                }

                if (isUpdate) {
                    if (CertUtil.isFDroidApp(appContext, BuildConfig.APPLICATION_ID)) {
                        if (selfUpdate.fdroidBuild.isNotEmpty()) {
                            return@withContext SelfUpdate.toApp(selfUpdate, appContext)
                        }
                    } else if (selfUpdate.auroraBuild.isNotEmpty()) {
                        return@withContext SelfUpdate.toApp(selfUpdate, appContext)
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

    private fun notifyUpdates(updates: List<Update>) {
        with(appContext.getSystemService<NotificationManager>()!!) {
            notify(
                notificationID,
                NotificationUtil.getUpdateNotification(appContext, updates)
            )
        }
    }
}
