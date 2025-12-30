package com.aurora.store.data.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller.EXTRA_UNARCHIVE_PACKAGE_NAME
import android.util.Log
import androidx.core.content.getSystemService
import com.aurora.extensions.TAG
import com.aurora.extensions.isVAndAbove
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.store.AuroraApp
import com.aurora.store.data.helper.DownloadHelper
import com.aurora.store.data.providers.AccountProvider
import com.aurora.store.util.NotificationUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Triggers re-install/unarchive of a previously archived app on Android 15+ devices.
 */
@AndroidEntryPoint
class UnarchivePackageReceiver: BroadcastReceiver() {

    @Inject
    lateinit var appDetailsHelper: AppDetailsHelper

    @Inject
    lateinit var downloadHelper: DownloadHelper

    override fun onReceive(context: Context?, intent: Intent?) {
        if (isVAndAbove && context != null && intent?.action == Intent.ACTION_UNARCHIVE_PACKAGE) {
            val packageName = intent.getStringExtra(EXTRA_UNARCHIVE_PACKAGE_NAME)!!
            Log.i(TAG, "Received request to unarchive $packageName")

            AuroraApp.scope.launch(Dispatchers.IO) {
                if (!AccountProvider.isLoggedIn(context)) {
                    Log.e(TAG, "Failed to authenticate user!")
                    with(context.getSystemService<NotificationManager>()!!) {
                        notify(
                            packageName.hashCode(),
                            NotificationUtil.getUnarchiveAuthNotification(context, packageName)
                        )
                    }
                    return@launch
                }

                val app = appDetailsHelper.getAppByPackageName(packageName)
                downloadHelper.enqueueApp(app)
            }
        }
    }
}
