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

package com.aurora.store.data.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aurora.store.BuildConfig
import com.aurora.store.data.downloader.RequestGroupIdBuilder
import com.aurora.store.data.event.BusEvent.InstallEvent
import com.aurora.store.data.event.BusEvent.UninstallEvent
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.util.PathUtil
import com.aurora.store.util.Preferences
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus
import java.io.File

@AndroidEntryPoint
open class PackageManagerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != null && intent.data != null) {
            val packageName = intent.data!!.encodedSchemeSpecificPart

            when (intent.action) {
                Intent.ACTION_PACKAGE_ADDED -> {
                    EventBus.getDefault().post(InstallEvent(packageName, ""))
                }

                Intent.ACTION_PACKAGE_REMOVED -> {
                    EventBus.getDefault().post(UninstallEvent(packageName, ""))
                }
            }

            //Clear installation queue
            AppInstaller.getInstance(context)
                .getPreferredInstaller()
                .removeFromInstallQueue(packageName)

            //clearNotification(context, packageName)

            val isAutoDeleteAPKEnabled = Preferences.getBoolean(
                context,
                Preferences.PREFERENCE_AUTO_DELETE
            )

            if (isAutoDeleteAPKEnabled)
                clearDownloads(context, packageName)

            //Clear self update apk
            if (packageName == BuildConfig.APPLICATION_ID)
                clearDownloads(context, packageName)
        }
    }

    private fun clearNotification(context: Context, packageName: String) {
        val notificationManager = context.applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val groupIDsOfPackageName = RequestGroupIdBuilder.getGroupIDsForApp(context, packageName.hashCode())
        groupIDsOfPackageName.forEach {
            notificationManager.cancel(packageName, it)
        }
    }

    private fun clearDownloads(context: Context, packageName: String) {
        try {
            val rootDirPath = PathUtil.getPackageDirectory(context, packageName)
            val rootDir = File(rootDirPath)
            if (rootDir.exists())
                rootDir.deleteRecursively()
        } catch (e: Exception) {

        }
    }
}
