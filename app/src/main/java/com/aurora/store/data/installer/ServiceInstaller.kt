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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.RemoteException
import androidx.annotation.RequiresApi
import com.aurora.services.IPrivilegedCallback
import com.aurora.services.IPrivilegedService
import com.aurora.store.BuildConfig
import com.aurora.store.util.Log
import java.io.File

class ServiceInstaller(context: Context) : InstallerBase(context) {

    companion object {
        const val ACTION_INSTALL_REPLACE_EXISTING = 2
        const val PRIVILEGED_EXTENSION_PACKAGE_NAME = "com.aurora.services"
        const val PRIVILEGED_EXTENSION_SERVICE_INTENT = "com.aurora.services.IPrivilegedService"
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun install(packageName: String, files: List<Any>) {
        if (isAlreadyQueued(packageName)) {
            Log.i("$packageName already queued")
        } else {
            Log.i("Received service install request for $packageName")
            val uriList = files.map {
                when (it) {
                    is File -> getUri(it)
                    is String -> getUri(File(it))
                    else -> {
                        throw Exception("Invalid data, expecting listOf() File or String")
                    }
                }
            }

            xInstall(packageName, uriList)
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun xInstall(packageName: String, uriList: List<Uri>) {

        val serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                val service = IPrivilegedService.Stub.asInterface(binder)
                val callback = object : IPrivilegedCallback.Stub() {
                    override fun handleResult(packageName: String, returnCode: Int) {
                        removeFromInstallQueue(packageName)
                    }
                }

                try {
                    service.installSplitPackage(
                        packageName,
                        uriList,
                        ACTION_INSTALL_REPLACE_EXISTING,
                        BuildConfig.APPLICATION_ID,
                        callback
                    )
                } catch (e: RemoteException) {
                    removeFromInstallQueue(packageName)
                    Log.e("Failed to connect Aurora Services")
                }
            }

            override fun onServiceDisconnected(name: ComponentName) {
                removeFromInstallQueue(packageName)
                Log.e("Disconnected from Aurora Services")
            }
        }

        val intent = Intent(PRIVILEGED_EXTENSION_SERVICE_INTENT)
        intent.setPackage(PRIVILEGED_EXTENSION_PACKAGE_NAME)

        context.bindService(
            intent,
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }
}