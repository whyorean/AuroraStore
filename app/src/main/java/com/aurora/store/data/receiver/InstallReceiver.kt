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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.aurora.Constants
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.util.PathUtil
import java.io.File
import kotlin.io.path.pathString

class InstallReceiver : BroadcastReceiver() {

    private val TAG = InstallReceiver::class.java.simpleName

    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.extras?.getString(Constants.STRING_APP) ?: String()
        val version = intent.extras?.getInt(Constants.STRING_VERSION)
        if (packageName.isNotBlank() && version != null) {
            try {
                val downloadDir =
                    File(PathUtil.getAppDownloadDir(context, packageName, version).pathString)
                AppInstaller.getInstance(context).getPreferredInstaller()
                    .install(
                        packageName,
                        downloadDir.listFiles()!!.filter { it.path.endsWith(".apk") }
                    )
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to install $packageName")
            }
        }
    }
}
