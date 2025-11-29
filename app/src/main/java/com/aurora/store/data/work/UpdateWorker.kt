package com.aurora.store.data.work

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.content.getSystemService
import androidx.hilt.work.HiltWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.aurora.Constants
import com.aurora.extensions.isIgnoringBatteryOptimizations
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.gplayapi.network.IHttpClient
import com.aurora.store.BuildConfig
import com.aurora.store.data.helper.DownloadHelper
import com.aurora.store.data.helper.UpdateHelper
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.data.model.BuildType
import com.aurora.store.data.model.SelfUpdate
import com.aurora.store.data.model.UpdateMode
import com.aurora.store.data.providers.AccountProvider
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.data.providers.BlacklistProvider
import com.aurora.store.data.room.update.Update
import com.aurora.store.data.room.update.UpdateDao
import com.aurora.store.util.CertUtil
import com.aurora.store.util.NotificationUtil
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_AUTO
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.Locale

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
    private val json: Json,
    private val blacklistProvider: BlacklistProvider,
    private val httpClient: IHttpClient,
    private val updateDao: UpdateDao,
    private val downloadHelper: DownloadHelper,
    private val authProvider: AuthProvider,
    private val appDetailsHelper: AppDetailsHelper,
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters
) : AuthWorker(authProvider, context, workerParams) {

    private val TAG = UpdateWorker::class.java.simpleName

    private val notificationID = 100

    private val canSelfUpdate = !CertUtil.isFDroidApp(context, BuildConfig.APPLICATION_ID) &&
            !CertUtil.isAppGalleryApp(context, BuildConfig.APPLICATION_ID) &&
            BuildType.CURRENT != BuildType.DEBUG

    private val isAuroraOnlyFilterEnabled: Boolean
        get() = Preferences.getBoolean(context, Preferences.PREFERENCE_FILTER_AURORA_ONLY, false)

    private val isFDroidFilterEnabled: Boolean
        get() = Preferences.getBoolean(context, Preferences.PREFERENCE_FILTER_FDROID)

    private val isExtendedUpdateEnabled: Boolean
        get() = Preferences.getBoolean(context, Preferences.PREFERENCE_UPDATES_EXTENDED)

    override suspend fun doWork(): Result {
        super.doWork()

        Log.i(TAG, "Checking for app updates")
        val updateMode = UpdateMode.entries[inputData.getInt(
            UpdateHelper.UPDATE_MODE,
            Preferences.getInteger(
                context,
                PREFERENCE_UPDATES_AUTO,
                UpdateMode.CHECK_AND_INSTALL.ordinal
            )
        )]

        if (updateMode == UpdateMode.DISABLED || !AccountProvider.isLoggedIn(context)) {
            Log.i(TAG, "Updates are disabled, bailing out!")
            return Result.failure()
        }

        if (!authProvider.isSavedAuthDataValid()) {
            Log.i(TAG, "AuthData is not valid, retrying later!")
            return Result.retry()
        }

        try {
            val updates = checkUpdates()
                .also { updateDao.insertUpdates(it) }
                .filter { if (!isExtendedUpdateEnabled) it.hasValidCert else true }

            if (updates.isEmpty() || updateMode == UpdateMode.CHECK_ONLY) {
                Log.i(TAG, "Found ${updates.size} updates")
                return Result.success()
            }

            // Notify and exit if we are only checking for updates or if battery optimizations are enabled
            if (updateMode == UpdateMode.CHECK_AND_NOTIFY || !context.isIgnoringBatteryOptimizations()) {
                Log.i(TAG, "Found  ${updates.size} updates, notifying!")
                notifyUpdates(updates)
                return Result.success()
            }

            // Clean the update list to prepare for installing
            val filteredUpdates = updates
                .filter { it.hasValidCert }
                .filterNot { it.isSelfUpdate(context) }
                .partition {
                    AppInstaller.canInstallSilently(context, it.packageName, it.targetSdk)
                }

            // Notify about apps that cannot be auto-updated
            if (filteredUpdates.second.isNotEmpty()) {
                Log.i(TAG, "Found  ${updates.size} updates out of which ${filteredUpdates.second.size} cannot be auto-updated")
                notifyUpdates(filteredUpdates.second)
            }

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
            NotificationUtil.getUpdateNotification(context)
        )
    }

    /**
     * Checks and returns updates for all possible apps
     */
    private suspend fun checkUpdates(): List<Update> {
        return withContext(Dispatchers.IO) {
            val packages = PackageUtil.getAllValidPackages(context)
                .filterNot { blacklistProvider.isBlacklisted(it.packageName) }
                .filter { if (!isExtendedUpdateEnabled) it.applicationInfo!!.enabled else true }

            // Filter out packages based on user's preferences
            val filteredPackages = if (isAuroraOnlyFilterEnabled) {
                packages.filter { CertUtil.isAuroraStoreApp(context, it.packageName) }
            } else {
                packages.filterNot { if (isFDroidFilterEnabled) CertUtil.isFDroidApp(context, it.packageName) else false }
            }

            val updates = appDetailsHelper.getAppByPackageName(filteredPackages.map { it.packageName })
                .filter { it.displayName.isNotEmpty() }
                .filter { PackageUtil.isUpdatable(context, it.packageName, it.versionCode.toLong()) }
                .toMutableList()

            if (canSelfUpdate) getSelfUpdate()?.let { updates.add(it) }

            return@withContext updates.map { Update.fromApp(context, it) }
                .sortedBy { it.displayName.lowercase(Locale.getDefault()) }
        }
    }

    /**
     * Checks and returns updates for Aurora Store if available
     */
    private suspend fun getSelfUpdate(): App? {
        return withContext(Dispatchers.IO) {
            val updateUrl = when (BuildType.CURRENT) {
                BuildType.RELEASE -> Constants.UPDATE_URL_STABLE
                BuildType.NIGHTLY -> Constants.UPDATE_URL_NIGHTLY
                else -> {
                    Log.i(TAG, "Self-updates are not available for this build!")
                    return@withContext null
                }
            }

            try {
                val response = httpClient.get(updateUrl, mapOf())
                val selfUpdate = json.decodeFromString<SelfUpdate>(String(response.responseBytes))

                val isUpdate = when (BuildType.CURRENT) {
                    BuildType.NIGHTLY,
                    BuildType.RELEASE -> selfUpdate.versionCode > BuildConfig.VERSION_CODE
                    else -> false
                }

                if (isUpdate) {
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

    private fun notifyUpdates(updates: List<Update>) {
        with(context.getSystemService<NotificationManager>()!!) {
            notify(
                notificationID,
                NotificationUtil.getUpdateNotification(context, updates)
            )
        }
    }
}
