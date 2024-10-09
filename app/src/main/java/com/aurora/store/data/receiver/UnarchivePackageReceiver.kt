package com.aurora.store.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller.EXTRA_UNARCHIVE_PACKAGE_NAME
import android.util.Log
import com.aurora.extensions.isVAndAbove

class UnarchivePackageReceiver: BroadcastReceiver() {

    private val TAG = UnarchivePackageReceiver::class.java.simpleName

    override fun onReceive(context: Context?, intent: Intent?) {
        if (isVAndAbove() && intent?.action == Intent.ACTION_UNARCHIVE_PACKAGE) {
            val packageName = intent.getStringExtra(EXTRA_UNARCHIVE_PACKAGE_NAME)!!
            Log.i(TAG, "Received request to unarchive $packageName")
        }
    }
}
