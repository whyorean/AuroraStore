package com.aurora.store.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.aurora.extensions.isSAndAbove
import com.aurora.store.data.work.CacheWorker
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_INTRO
import com.aurora.store.util.Preferences.PREFERENCE_MIGRATION_VERSION
import com.aurora.store.util.Preferences.PREFERENCE_THEME_ACCENT
import com.aurora.store.util.save
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MigrationReceiver: BroadcastReceiver() {

    private val TAG = MigrationReceiver::class.java.simpleName

    private val prefVersion = 1 // BUMP THIS MANUALLY ON ADDING NEW MIGRATION STEP

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val oldVersion = Preferences.getInteger(context, PREFERENCE_MIGRATION_VERSION)

            // Run required migrations on version upgrade for existing installs
            if (Preferences.getBoolean(context, PREFERENCE_INTRO) && oldVersion != prefVersion) {
                val newVersion = upgradeIfNeeded(context, oldVersion)
                Preferences.putInteger(context, PREFERENCE_MIGRATION_VERSION, newVersion)
            }
        }
    }

    private fun upgradeIfNeeded(context: Context, oldVersion: Int): Int {
        Log.i(TAG, "Upgrading from version $oldVersion to version $prefVersion")
        var currentVersion = oldVersion

        // Add new migrations / defaults below this point
        // Always bump currentVersion at the end of migration for next release

        // 58 -> 59
        if (currentVersion == 0) {
            CacheWorker.scheduleAutomatedCacheCleanup(context)
            if (isSAndAbove()) context.save(PREFERENCE_THEME_ACCENT, 0)
            currentVersion++
        }

        // Add new migrations / defaults above this point.
        if (currentVersion != prefVersion) {
            Log.e(TAG, "Upgrading to version $prefVersion left it at $currentVersion instead")
        } else {
            Log.i(TAG, "Finished running required migrations!")
        }

        return currentVersion
    }
}
