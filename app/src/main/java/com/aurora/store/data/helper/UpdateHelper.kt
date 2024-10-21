package com.aurora.store.data.helper

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OutOfQuotaPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.aurora.extensions.isMAndAbove
import com.aurora.store.AuroraApp
import com.aurora.store.data.event.BusEvent
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.data.model.UpdateMode
import com.aurora.store.data.room.update.UpdateDao
import com.aurora.store.data.work.UpdateWorker
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_CHECK_INTERVAL
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit.HOURS
import java.util.concurrent.TimeUnit.MINUTES
import javax.inject.Inject
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Helper class to work with the [UpdateWorker].
 */
class UpdateHelper @Inject constructor(
    private val updateDao: UpdateDao,
    @ApplicationContext private val context: Context
) {

    companion object {
        const val UPDATE_MODE = "UPDATE_MODE"

        private const val UPDATE_WORKER = "UPDATE_WORKER"
        private const val EXPEDITED_UPDATE_WORKER = "EXPEDITED_UPDATE_WORKER"
    }

    private val TAG = UpdateHelper::class.java.simpleName

    private val isExtendedUpdateEnabled
        get() = Preferences.getBoolean(context, Preferences.PREFERENCE_UPDATES_EXTENDED)

    val updates = updateDao.updates()
        .map { list -> if (!isExtendedUpdateEnabled) list.filter { it.hasValidCert } else list }
        .stateIn(AuroraApp.scope, SharingStarted.WhileSubscribed(), null)

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
        Log.i(TAG,"Scheduling periodic app updates!")
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                UPDATE_WORKER,
                ExistingPeriodicWorkPolicy.KEEP,
                getAutoUpdateWork()
            )
    }

    /**
     * Updates the automated updates check to reconsider the new user preferences
     * @see [UpdateWorker]
     */
    fun updateAutomatedCheck() {
        Log.i(TAG,"Updating periodic app updates!")
        WorkManager.getInstance(context).updateWork(getAutoUpdateWork())
    }

    private fun getAutoUpdateWork(): PeriodicWorkRequest {
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

    private suspend fun deleteInvalidUpdates() {
        updateDao.updates().firstOrNull()?.forEach { update ->
            if (!update.isInstalled(context) || update.isUpToDate(context)) {
                deleteUpdate(update.packageName)
            }
        }
    }
}
