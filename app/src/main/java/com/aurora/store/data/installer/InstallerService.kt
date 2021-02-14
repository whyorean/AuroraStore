/*
 * Aurora Store
 *  Copyright (C) 2021, Rahul Kumar Patel <whyorean@gmail.com>
 *
 *  Aurora Store is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Aurora Store is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.aurora.store.data.installer

import android.app.Service
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import org.apache.commons.lang3.StringUtils

class InstallerService : Service() {

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
        val packageName = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME)

        //Send broadcast for the installation status of the package
        sendStatusBroadcast(status, packageName)

        //Launch user confirmation activity
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            val confirmationIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
            confirmationIntent!!.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            confirmationIntent.putExtra(
                Intent.EXTRA_INSTALLER_PACKAGE_NAME,
                "com.android.vending"
            )
            confirmationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                startActivity(confirmationIntent)
            } catch (e: Exception) {
                sendStatusBroadcast(PackageInstaller.STATUS_FAILURE, packageName)
            }
        }
        stopSelf()
        return START_NOT_STICKY
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun sendStatusBroadcast(status: Int, packageName: String?) {
        if (StringUtils.isNotEmpty(packageName)) {
            val statusIntent = Intent(ACTION_SESSION_INSTALLER)
            statusIntent.putExtra(PackageInstaller.EXTRA_STATUS, status)
            statusIntent.putExtra(PackageInstaller.EXTRA_PACKAGE_NAME, packageName)
            sendBroadcast(statusIntent)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {
        private const val ACTION_SESSION_INSTALLER = "ACTION_SESSION_INSTALLER"
    }
}