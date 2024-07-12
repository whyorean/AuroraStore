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
import com.aurora.store.AuroraApp
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.data.installer.AppInstaller
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
open class PackageManagerReceiver : BroadcastReceiver() {

    @Inject
    lateinit var appInstaller: AppInstaller

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != null && intent.data != null) {
            val packageName = intent.data!!.encodedSchemeSpecificPart

            when (intent.action) {
                Intent.ACTION_PACKAGE_ADDED -> {
                    AuroraApp.flowEvent.emitEvent(InstallerEvent.Installed(packageName))
                }

                Intent.ACTION_PACKAGE_REMOVED -> {
                    AuroraApp.flowEvent.emitEvent(InstallerEvent.Uninstalled(packageName))
                }
            }

            //Clear installation queue
            appInstaller.getPreferredInstaller().removeFromInstallQueue(packageName)
        }
    }
}
