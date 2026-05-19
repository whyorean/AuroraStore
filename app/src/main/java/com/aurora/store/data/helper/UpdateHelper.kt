package com.aurora.store.data.helper

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.aurora.extensions.TAG
import com.aurora.store.AuroraApp
import com.aurora.store.data.event.BusEvent
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.data.model.UpdateMode
import com.aurora.store.data.room.update.IgnoredUpdate
import com.aurora.store.data.room.update.IgnoredUpdateDao
import com.aurora.store.data.room.update.Update
import com.aurora.store.data.room.update.UpdateDao
import com.aurora.store.data.work.UpdateWorker
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCES_UPDATES_RESTRICTIONS_BATTERY
import com.aurora.store.util.Preferences.PREFERENCES_UPDATES_RESTRICTIONS_IDLE
import com.aurora.store.util.Preferences.PREFERENCES_UPDATES_RESTRICTIONS_METERED
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_CHECK_INTERVAL
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import java.util.concurrent.TimeUnit.HOURS
import java.util.concurrent.TimeUnit.MINUTES
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Helper class to work with the [UpdateWorker].
 */
class UpdateHelper @Inject constructor(
    private val updateDao: UpdateDao,
    private val ignoredUpdateDao: IgnoredUpdateDao,
    @ApplicationContext private val context: Context
) {

    companion object {
        const val UPDATE_MODE = "UPDATE_MODE"

        private const val UPDATE_WORKER = "UPDATE_WORKER"
        private const val EXPEDITED_UPDATE_WORKER = "EXPEDITED_UPDATE_WORKER"

        fun getAutoUpdateWork(context: Context): PeriodicWorkRequest {
            val updateCheckInterval = Preferences.getInteger(
                context,
                PREFERENCE_UPDATES_CHECK_INTERVAL,
                3
            ).toLong()

            val constraints = Constraints.Builder()

            if (Preferences.getBoolean(context, PREFERENCES_UPDATES_RESTRICTIONS_METERED, true)) {
                constraints.setRequiredNetworkType(NetworkType.UNMETERED)
            }

            if (Preferences.getBoolean(context, PREFERENCES_UPDATES_RESTRICTIONS_BATTERY, true)) {
                constraints.setRequiresBatteryNotLow(true)
            }

            if (Preferences.getBoolean(context, PREFERENCES_UPDATES_RESTRICTIONS_IDLE, true)) {
                constraints.setRequiresDeviceIdle(true)
            }

            return PeriodicWorkRequestBuilder<UpdateWorker>(
                repeatInterval = updateCheckInterval,
                repeatIntervalTimeUnit = HOURS,
                flexTimeInterval = 30,
                flexTimeIntervalUnit = MINUTES
            ).setConstraints(constraints.build())
                .setId(UUID.nameUUIDFromBytes(UPDATE_WORKER.toByteArray()))
                .build()
        }
    }

    private val isExtendedUpdateEnabled
        get() = Preferences.getBoolean(context, Preferences.PREFERENCE_UPDATES_EXTENDED)

    private val filteredUpdates = updateDao.updates()
        .map { list -> if (!isExtendedUpdateEnabled) list.filter { it.hasValidCert } else list }

    /**
     * Updates surfaced to the UI / notifications. Updates muted by the user via
     * [ignoreAll] or [ignoreVersion] are filtered out — see [ignoredUpdates] for the
     * complementary list.
     */
    val updates = combine(filteredUpdates, ignoredUpdateDao.ignoredUpdates()) { list, ignored ->
        val byPkg = ignored.associateBy { it.packageName }
        list.filterNot { it.isIgnoredBy(byPkg[it.packageName]) }
    }.stateIn(AuroraApp.scope, SharingStarted.WhileSubscribed(), null)

    /**
     * Updates that the user has chosen to ignore (all-future or version-specific) and
     * which currently have a row in the `update` table. Used to render the "Ignored"
     * footer section on the Updates screen.
     */
    val ignoredUpdates = combine(
        filteredUpdates,
        ignoredUpdateDao.ignoredUpdates()
    ) { list, ignored ->
        val byPkg = ignored.associateBy { it.packageName }
        list.filter { it.isIgnoredBy(byPkg[it.packageName]) }
    }.stateIn(AuroraApp.scope, SharingStarted.WhileSubscribed(), emptyList())

    private fun Update.isIgnoredBy(rule: IgnoredUpdate?): Boolean {
        if (rule == null) return false
        return rule.ignoredVersionCode == null || rule.ignoredVersionCode == versionCode
    }

    val isCheckingUpdates = WorkManager.getInstance(context)
        .getWorkInfosForUniqueWorkFlow(EXPEDITED_UPDATE_WORKER)
        .map { list -> !list.all { it.state.isFinished } }
        .stateIn(AuroraApp.scope, SharingStarted.WhileSubscribed(), false)

    /**
     * Deletes invalid updates from database and starts observing events
     */
    fun init() {
        AuroraApp.scope.launch {
            deleteInvalidUpdates()
        }.invokeOnCompletion {
            observeUpdates()
        }
    }

    private fun observeUpdates() {
        AuroraApp.events.installerEvent.onEach {
            when (it) {
                is InstallerEvent.Installed -> deleteUpdate(it.packageName)
                is InstallerEvent.Uninstalled -> deleteUpdate(it.packageName)
                else -> {}
            }
        }.launchIn(AuroraApp.scope)

        AuroraApp.events.busEvent.onEach {
            if (it is BusEvent.Blacklisted) deleteUpdate(it.packageName)
        }.launchIn(AuroraApp.scope)
    }

    /**
     * Checks for updates using an expedited worker
     */
    fun checkUpdatesNow() {
        val inputData = Data.Builder()
            .putInt(UPDATE_MODE, UpdateMode.CHECK_ONLY.ordinal)
            .build()

        val work = OneTimeWorkRequestBuilder<UpdateWorker>()
            .addTag(EXPEDITED_UPDATE_WORKER)
            .setExpedited(OutOfQuotaPolicy.DROP_WORK_REQUEST)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(EXPEDITED_UPDATE_WORKER, ExistingWorkPolicy.KEEP, work)
    }

    /**
     * Delete update for a package from the database
     * @param packageName Name of the package
     */
    private suspend fun deleteUpdate(packageName: String) {
        updateDao.delete(packageName)
    }

    /**
     * Delete all updates from the database
     */
    suspend fun deleteAllUpdates() {
        updateDao.deleteAll()
    }

    /**
     * Hide all future updates for [packageName] until [unignore] is called.
     */
    suspend fun ignoreAll(packageName: String) {
        ignoredUpdateDao.upsert(IgnoredUpdate(packageName = packageName))
    }

    /**
     * Hide only [versionCode] for [packageName]; newer versions show up again.
     */
    suspend fun ignoreVersion(packageName: String, versionCode: Long) {
        ignoredUpdateDao.upsert(
            IgnoredUpdate(packageName = packageName, ignoredVersionCode = versionCode)
        )
    }

    /**
     * Drop the ignore rule for [packageName], regardless of which kind it was.
     */
    suspend fun unignore(packageName: String) {
        ignoredUpdateDao.delete(packageName)
    }

    /**
     * Cancels the automated updates check
     * @see [UpdateWorker]
     */
    fun cancelAutomatedCheck() {
        Log.i(TAG, "Cancelling periodic app updates!")
        WorkManager.getInstance(context).cancelUniqueWork(UPDATE_WORKER)
    }

    /**
     * Schedules the automated updates check
     * @see [UpdateWorker]
     */
    fun scheduleAutomatedCheck() {
        Log.i(TAG, "Scheduling periodic app updates!")
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                UPDATE_WORKER,
                ExistingPeriodicWorkPolicy.KEEP,
                getAutoUpdateWork(context)
            )
    }

    /**
     * Updates the automated updates check to reconsider the new user preferences
     * @see [UpdateWorker]
     */
    fun updateAutomatedCheck() {
        Log.i(TAG, "Updating periodic app updates!")
        runCatching { WorkManager.getInstance(context).updateWork(getAutoUpdateWork(context)) }
            .onFailure { Log.e(TAG, "Failed to update periodic app updates!", it) }
    }

    private suspend fun deleteInvalidUpdates() {
        updateDao.updates().firstOrNull()?.forEach { update ->
            if (!update.isInstalled(context) || update.isUpToDate(context)) {
                deleteUpdate(update.packageName)
            }
        }
    }
}
