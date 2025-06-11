package com.aurora.store.data.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.getSystemService
import androidx.work.WorkManager
import com.aurora.Constants
import com.aurora.extensions.isOAndAbove
import com.aurora.store.data.helper.UpdateHelper.Companion.getAutoUpdateWork
import com.aurora.store.data.model.UpdateMode
import com.aurora.store.data.work.CacheWorker
import com.aurora.store.util.CertUtil
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_DISPENSER_URLS
import com.aurora.store.util.Preferences.PREFERENCE_INTRO
import com.aurora.store.util.Preferences.PREFERENCE_MIGRATION_VERSION
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_AUTO
import com.aurora.store.util.Preferences.PREFERENCE_VENDING_VERSION
import com.aurora.store.util.save
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MigrationReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "MigrationReceiver"

        private const val PREF_VERSION = 3 // BUMP THIS MANUALLY ON ADDING NEW MIGRATION STEP

        fun runMigrationsIfRequired(context: Context) {
            val oldVersion = Preferences.getInteger(context, PREFERENCE_MIGRATION_VERSION)

            // Run required migrations on version upgrade for existing installs
            if (Preferences.getBoolean(context, PREFERENCE_INTRO) && oldVersion != PREF_VERSION) {
                val newVersion = upgradeIfNeeded(context, oldVersion)
                Preferences.putInteger(context, PREFERENCE_MIGRATION_VERSION, newVersion)
            } else {
                Log.i(TAG, "No need to run migrations")
            }
        }

        private fun upgradeIfNeeded(context: Context, oldVersion: Int): Int {
            Log.i(TAG, "Upgrading from version $oldVersion to version $PREF_VERSION")
            var currentVersion = oldVersion

            // Add new migrations / defaults below this point
            // Always bump currentVersion at the end of migration for next release

            // 58 -> 59
            if (currentVersion == 0) {
                CacheWorker.scheduleAutomatedCacheCleanup(context) // !1089
                context.save(PREFERENCE_DISPENSER_URLS, setOf(Constants.URL_DISPENSER)) // !1117
                if (Preferences.getInteger(context, PREFERENCE_VENDING_VERSION) == -1) {
                    context.save(PREFERENCE_VENDING_VERSION, 0) // !1049
                }
                currentVersion++
            }

            // 59 -> 60
            if (currentVersion == 1) {
                if (CertUtil.isAppGalleryApp(context, context.packageName)) {
                    val dispensers = Preferences.getStringSet(context, PREFERENCE_DISPENSER_URLS)
                        .toMutableSet()
                    
                    dispensers.remove(Constants.URL_DISPENSER)

                    context.save(PREFERENCE_DISPENSER_URLS, dispensers)
                }
                currentVersion++
            }

            // 63 -> 64
            if (currentVersion == 2) {
                if (isOAndAbove) {
                    with(context.getSystemService<NotificationManager>()!!) { // !1189
                        deleteNotificationChannel("NOTIFICATION_CHANNEL_GENERAL")
                        deleteNotificationChannel("NOTIFICATION_CHANNEL_ALERT")
                    }
                }
                currentVersion++
            }

            // 68 -> 69
            if (currentVersion == 3) {
                val updateMode = UpdateMode.entries[
                    Preferences.getInteger(
                    context,
                    PREFERENCE_UPDATES_AUTO,
                    UpdateMode.DISABLED.ordinal
                )]

                if (updateMode != UpdateMode.DISABLED) {
                    runCatching {
                        WorkManager.getInstance(context).updateWork(getAutoUpdateWork(context))
                    }.onFailure { Log.e(TAG, "Failed to migrate app updates!", it) }
                }
                currentVersion++
            }

            // Add new migrations / defaults above this point.
            if (currentVersion != PREF_VERSION) {
                Log.e(TAG, "Upgrading to version $PREF_VERSION left it at $currentVersion instead")
            } else {
                Log.i(TAG, "Finished running required migrations!")
            }

            return currentVersion
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            runMigrationsIfRequired(context)
        }
    }
}
