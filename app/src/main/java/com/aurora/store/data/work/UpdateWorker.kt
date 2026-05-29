package com.aurora.store.data.work

import android.app.NotificationManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.core.content.getSystemService
import androidx.hilt.work.HiltWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.aurora.Constants
import com.aurora.extensions.TAG
import com.aurora.extensions.isGrapheneOS
import com.aurora.extensions.isHyperOS
import com.aurora.extensions.isIgnoringBatteryOptimizations
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.store.BuildConfig
import com.aurora.store.data.helper.DownloadHelper
import com.aurora.store.data.helper.UpdateHelper
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.data.model.BuildType
import com.aurora.store.data.model.SelfUpdate
import com.aurora.store.data.model.UpdateMode
import com.aurora.store.data.network.HttpClient
import com.aurora.store.data.providers.AccountProvider
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.data.providers.BlacklistProvider
import com.aurora.store.data.room.update.Update
import com.aurora.store.data.room.update.UpdateDao
import com.aurora.store.util.CertUtil
import com.aurora.store.util.NotificationUtil
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_SELF_UPDATE_ENABLED
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_AUTO
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * A worker that drives periodic app-update checks. The repeat interval is configurable
 * by the user, defaulting to 3 hours with a flex time of 30 minutes.
 *
 * Aurora Store's own update is fetched from the bundled release/nightly feed and added
 * to the regular update list (see [getSelfUpdate]); from there it reuses the standard
 * download + install pipeline. It is never auto-installed silently — the user triggers
 * it from the Updates tab.
 *
 * Avoid using this worker directly and prefer using [UpdateHelper] instead.
 * @see AuthWorker
 */
@HiltWorker
class UpdateWorker @AssistedInject constructor(
    private val httpClient: HttpClient,
    private val json: Json,
    private val blacklistProvider: BlacklistProvider,
    private val updateDao: UpdateDao,
    private val downloadHelper: DownloadHelper,
    private val authProvider: AuthProvider,
    private val appDetailsHelper: AppDetailsHelper,
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters
) : AuthWorker(authProvider, context, workerParams) {

    companion object {
        private const val NOTIFICATION_ID = 100
    }

    /**
     * `true` when the build supports self-update ([PackageUtil.isSelfUpdateSupported])
     * and the user hasn't opted out via the Settings toggle. Read each check so flipping
     * the preference takes effect on the next run.
     */
    private val canSelfUpdate: Boolean
        get() = PackageUtil.isSelfUpdateSupported(context) &&
            Preferences.getBoolean(context, PREFERENCE_SELF_UPDATE_ENABLED, true)

    private val isAuroraOnlyFilterEnabled: Boolean
        get() = Preferences.getBoolean(context, Preferences.PREFERENCE_FILTER_AURORA_ONLY, false)

    private val isFDroidFilterEnabled: Boolean
        get() = Preferences.getBoolean(context, Preferences.PREFERENCE_FILTER_FDROID)

    private val isExtendedUpdateEnabled: Boolean
        get() = Preferences.getBoolean(context, Preferences.PREFERENCE_UPDATES_EXTENDED)

    override suspend fun doWork(): Result {
        super.doWork()

        Log.i(TAG, "Checking for app updates")
        val updateMode = UpdateMode.entries[
            inputData.getInt(
                UpdateHelper.UPDATE_MODE,
                Preferences.getInteger(
                    context,
                    PREFERENCE_UPDATES_AUTO,
                    UpdateMode.CHECK_AND_INSTALL.ordinal
                )
            )
        ]

        if (updateMode == UpdateMode.DISABLED || !AccountProvider.isLoggedIn(context)) {
            Log.i(TAG, "Updates are disabled, bailing out!")
            return Result.failure()
        }

        if (!authProvider.isSavedAuthDataValid()) {
            Log.i(TAG, "AuthData is not valid, retrying later!")
            return Result.retry()
        }

        try {
            val allUpdates = checkUpdates()
                .also { updateDao.insertUpdates(it) }
                .filter { if (!isExtendedUpdateEnabled) it.hasValidCert else true }

            // Incompatible updates (e.g. system app updates on HyperOS / GrapheneOS) are
            // surfaced in the UI but excluded from notifications and auto-install.
            val updates = allUpdates.filterNot { it.isIncompatible }

            if (updates.isEmpty() || updateMode == UpdateMode.CHECK_ONLY) {
                Log.i(TAG, "Found ${updates.size} updates")
                return Result.success()
            }

            // Notify and exit if we are only checking for updates or if battery optimizations are enabled
            if (updateMode == UpdateMode.CHECK_AND_NOTIFY ||
                !context.isIgnoringBatteryOptimizations()
            ) {
                Log.i(TAG, "Found  ${updates.size} updates, notifying!")
                notifyUpdates(updates)
                return Result.success()
            }

            // Clean the update list to prepare for installing. Aurora Store's own update
            // is never installed silently — the user triggers it from the Updates tab.
            val filteredUpdates = updates
                .filter { it.hasValidCert }
                .filterNot { it.isSelfUpdate(context) }
                .partition {
                    AppInstaller.canInstallSilently(context, it.packageName, it.targetSdk)
                }

            // Notify about apps that cannot be auto-updated
            if (filteredUpdates.second.isNotEmpty()) {
                Log.i(
                    TAG,
                    "Found  ${updates.size} updates out of which ${filteredUpdates.second.size} cannot be auto-updated"
                )
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

    override suspend fun getForegroundInfo(): ForegroundInfo = ForegroundInfo(
        NOTIFICATION_ID,
        NotificationUtil.getUpdateNotification(context)
    )

    /**
     * Checks and returns updates for all possible apps
     */
    private suspend fun checkUpdates(): List<Update> {
        return withContext(Dispatchers.IO) {
            val packages = PackageUtil.getAllValidPackages(context)
                .filterNot { blacklistProvider.isBlacklisted(it.packageName) }
                .filter { if (!isExtendedUpdateEnabled) it.applicationInfo!!.enabled else true }

            // Filter out packages based on user's preferences
            val installerFilters = Preferences.getStringSet(
                context,
                Preferences.PREFERENCE_FILTER_INSTALLERS
            )
            val filteredPackages = if (isAuroraOnlyFilterEnabled) {
                packages.filter { CertUtil.isAuroraStoreApp(context, it.packageName) }
            } else {
                packages.filterNot { pkg ->
                    if (isFDroidFilterEnabled && CertUtil.isFDroidApp(context, pkg.packageName)) {
                        return@filterNot true
                    }
                    if (installerFilters.isNotEmpty()) {
                        val installer = PackageUtil.getInstallerPackageName(
                            context,
                            pkg.packageName
                        )
                        installer != null && installer in installerFilters
                    } else {
                        false
                    }
                }
            }.map { it.packageName }

            // HyperOS and GrapheneOS block third-party updates of pristine system apps.
            val osBlocksSystemAppUpdates = isHyperOS || isGrapheneOS
            val pristineSystemPackages: Set<String> = if (osBlocksSystemAppUpdates) {
                packages.filter {
                    val flags = it.applicationInfo?.flags ?: 0
                    flags and ApplicationInfo.FLAG_SYSTEM != 0 &&
                        flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP == 0
                }.map { it.packageName }.toSet()
            } else {
                emptySet()
            }

            val updates = appDetailsHelper.getAppByPackageName(filteredPackages)
                .filter { it.displayName.isNotEmpty() }
                .filter { PackageUtil.isUpdatable(context, it.packageName, it.versionCode) }
                .toMutableList()

            // Aurora Store's own update comes from the feed, not Play. When one is
            // offered, add it; otherwise drop any stale self-update row. This is the
            // cleanup path for the row (nightly self-updates are exempt from
            // deleteInvalidUpdates, and the install event isn't delivered reliably when
            // the app replaces itself), so a previously shown self-update doesn't linger
            // after we've already updated to it.
            val selfUpdate = if (canSelfUpdate) getSelfUpdate() else null
            if (selfUpdate != null) {
                updates.add(selfUpdate)
            } else {
                updateDao.delete(context.packageName)
            }

            return@withContext updates.map {
                Update.fromApp(
                    context,
                    it,
                    isIncompatible = it.packageName in pristineSystemPackages
                )
            }.sortedBy { it.displayName.lowercase(Locale.getDefault()) }
        }
    }

    /**
     * Fetches Aurora Store's own update from the bundled release/nightly feed and maps
     * it onto an [App] so it joins the regular update list. Nightly version codes never
     * bump, so newness is decided by the build timestamp there; release uses the version
     * code. Best-effort: any failure logs and yields no update.
     */
    private suspend fun getSelfUpdate(): App? = withContext(Dispatchers.IO) {
        val updateUrl = when (BuildType.CURRENT) {
            BuildType.RELEASE -> Constants.UPDATE_URL_VANILLA
            BuildType.NIGHTLY -> Constants.UPDATE_URL_NIGHTLY
            else -> {
                Log.i(TAG, "Self-updates are not available for this build!")
                return@withContext null
            }
        }

        try {
            val selfUpdate = httpClient.call(updateUrl).use {
                json.decodeFromString<SelfUpdate>(it.body.string())
            }

            val isNewer = when (BuildType.CURRENT) {
                BuildType.RELEASE -> selfUpdate.versionCode > BuildConfig.VERSION_CODE
                BuildType.NIGHTLY -> selfUpdate.timestamp > BuildConfig.BUILD_TIMESTAMP
                else -> false
            }

            if (isNewer && selfUpdate.downloadUrl.isNotBlank()) {
                return@withContext selfUpdate.toApp(context)
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to check self-updates", exception)
        }

        Log.i(TAG, "No self-updates found!")
        return@withContext null
    }

    private fun notifyUpdates(updates: List<Update>) {
        with(context.getSystemService<NotificationManager>()!!) {
            notify(
                NOTIFICATION_ID,
                NotificationUtil.getUpdateNotification(context, updates)
            )
        }
    }
}
