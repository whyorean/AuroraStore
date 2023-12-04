/*
 * Aurora Store
 *  Copyright (C) 2021, Rahul Kumar Patel <whyorean@gmail.com>
 *  Copyright (C) 2023, grrfe <grrfe@420blaze.it>
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

package com.aurora.store

import android.app.Application
import androidx.core.content.ContextCompat
import com.aurora.extensions.isPAndAbove
import com.aurora.gplayapi.data.models.App
import com.aurora.store.data.downloader.DownloadManager
import com.aurora.store.data.receiver.PackageManagerReceiver
import com.aurora.store.data.service.NotificationService
import com.aurora.store.data.work.DownloadWorker
import com.aurora.store.util.CommonUtil
import com.aurora.store.util.NotificationUtil
import com.aurora.store.util.PackageUtil
import com.tonyodev.fetch2.Fetch
import dagger.hilt.android.HiltAndroidApp
import org.lsposed.hiddenapibypass.HiddenApiBypass

@HiltAndroidApp
class AuroraApplication : Application() {

    private lateinit var fetch: Fetch

    companion object{
        val enqueuedDownloads = mutableSetOf<App>()
        val enqueuedInstalls: MutableSet<String> = mutableSetOf()
    }

    override fun onCreate() {
        super.onCreate()

        // TODO: Only exempt required APIs
        // Required for Shizuku installer
        if (isPAndAbove()) {
            HiddenApiBypass.addHiddenApiExemptions("")
        }

        //Create Notification Channels : General & Alert
        NotificationUtil.createNotificationChannel(this)
        NotificationService.startService(this)

        fetch = DownloadManager.with(this).fetch

        // Initialize DownloadWorker to observe and trigger downloads
        DownloadWorker.initDownloadWorker(applicationContext)

        //Register broadcast receiver for package install/uninstall
        ContextCompat.registerReceiver(
            this,
            object : PackageManagerReceiver() {},
            PackageUtil.getFilter(),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        CommonUtil.cleanupInstallationSessions(applicationContext)
    }

}
