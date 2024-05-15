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
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.RemoteException
import androidx.core.content.FileProvider
import com.aurora.services.IPrivilegedCallback
import com.aurora.services.IPrivilegedService
import com.aurora.store.AuroraApp
import com.aurora.store.BuildConfig
import com.aurora.store.R
import com.aurora.store.data.event.BusEvent
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.data.model.InstallerInfo
import com.aurora.store.data.room.download.Download
import com.aurora.store.util.Log
import com.aurora.store.util.PackageUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Deprecated("Deprecated in favour of RootInstaller")
class ServiceInstaller @Inject constructor(
    @ApplicationContext context: Context
) : InstallerBase(context) {

    private lateinit var serviceConnection: ServiceConnection
    private val executor = ThreadPoolExecutor(0, 1, 30L, TimeUnit.SECONDS, LinkedBlockingQueue())

    companion object {
        const val ACTION_INSTALL_REPLACE_EXISTING = 2
        const val PRIVILEGED_EXTENSION_PACKAGE_NAME = "com.aurora.services"
        const val PRIVILEGED_EXTENSION_SERVICE_INTENT = "com.aurora.services.IPrivilegedService"

        fun getInstallerInfo(context: Context): InstallerInfo {
            return InstallerInfo(
                id = 3,
                title = context.getString(R.string.pref_install_mode_services),
                subtitle = context.getString(R.string.services_installer_subtitle),
                description = context.getString(R.string.services_installer_desc)
            )
        }
    }

    override fun install(download: Download) {
        super.install(download)

        when {
            isAlreadyQueued(download.packageName) -> {
                Log.i("${download.packageName} already queued")
            }

            PackageUtil.isInstalled(context, PRIVILEGED_EXTENSION_PACKAGE_NAME) -> {
                Log.i("Received service install request for ${download.packageName}")
                val fileList = getFiles(download.packageName, download.versionCode)
                xInstall(
                    download.packageName,
                    fileList.map { getUri(it) },
                    fileList.map { it.absolutePath }
                )
            }
            else -> {
                postError(
                    download.packageName,
                    context.getString(R.string.installer_status_failure),
                    context.getString(R.string.installer_service_unavailable)
                )
            }
        }
    }

    private fun xInstall(packageName: String, uriList: List<Uri>, fileList: List<String>) {
        executor.execute {
            val readyWithAction = AtomicBoolean(false)
            Handler(Looper.getMainLooper()).post {
                serviceConnection = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                        if (isAlreadyQueued(packageName)) {
                            if (::serviceConnection.isInitialized) {
                                context.unbindService(serviceConnection)
                            }
                            readyWithAction.set(true)
                            return
                        }
                        AuroraApp.enqueuedInstalls.add(packageName)
                        val service = IPrivilegedService.Stub.asInterface(binder)

                        if (service.hasPrivilegedPermissions()) {
                            Log.i(context.getString(R.string.installer_service_available))

                            val callback = object : IPrivilegedCallback.Stub() {

                                override fun handleResult(packageName: String, returnCode: Int) {}

                                override fun handleResultX(
                                    packageName: String,
                                    returnCode: Int,
                                    extra: String?
                                ) {
                                    removeFromInstallQueue(packageName)
                                    handleCallback(packageName, returnCode, extra)
                                    readyWithAction.set(true)
                                }
                            }

                            try {
                                if (service.isMoreMethodImplemented) {
                                    try {
                                        service.installSplitPackageMore(
                                            packageName,
                                            uriList,
                                            ACTION_INSTALL_REPLACE_EXISTING,
                                            BuildConfig.APPLICATION_ID,
                                            callback,
                                            fileList
                                        )
                                    } catch (e: RemoteException) {
                                        removeFromInstallQueue(packageName)
                                        postError(packageName, e.localizedMessage, e.stackTraceToString())
                                        readyWithAction.set(true)
                                    }
                                } else {
                                    throw Exception("New method not implemented")
                                }
                            } catch (th: Throwable) {
                                th.printStackTrace()
                                try {
                                    service.installSplitPackageX(
                                        packageName,
                                        uriList,
                                        ACTION_INSTALL_REPLACE_EXISTING,
                                        BuildConfig.APPLICATION_ID,
                                        callback
                                    )
                                } catch (e: RemoteException) {
                                    removeFromInstallQueue(packageName)
                                    postError(packageName, e.localizedMessage, e.stackTraceToString())
                                    readyWithAction.set(true)
                                }
                            }
                        } else {
                            removeFromInstallQueue(packageName)
                            postError(
                                packageName,
                                context.getString(R.string.installer_status_failure),
                                context.getString(R.string.installer_service_misconfigured)
                            )
                            readyWithAction.set(true)
                        }
                    }

                    override fun onServiceDisconnected(name: ComponentName) {
                        removeFromInstallQueue(packageName)
                        readyWithAction.set(true)
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
            while (!readyWithAction.get()) {
                Thread.sleep(1000)
            }
            Log.i("Services Callback : install wait done")
        }
    }

    private fun handleCallbackUninstall(packageName: String, returnCode: Int, extra: String?) {
        Log.i("Services Callback : $packageName $returnCode $extra")

        try {
            when (returnCode) {
                PackageInstaller.STATUS_SUCCESS -> {
                    EventBus.getDefault().post(
                        BusEvent.UninstallEvent(
                            packageName,
                            context.getString(R.string.installer_status_success)
                        )
                    )
                }
                else -> {
                    val error = AppInstaller.getErrorString(
                        context,
                        returnCode
                    )

                    postError(packageName, error, extra)
                }
            }
            if (::serviceConnection.isInitialized) {
                context.unbindService(serviceConnection)
            }
        } catch (th: Throwable) {
            th.printStackTrace()
        }
    }

    private fun handleCallback(packageName: String, returnCode: Int, extra: String?) {
        Log.i("Services Callback : $packageName $returnCode $extra")

        try {
            when (returnCode) {
                PackageInstaller.STATUS_SUCCESS -> {
                    EventBus.getDefault().post(
                        InstallerEvent.Success(
                            packageName,
                            context.getString(R.string.installer_status_success)
                        )
                    )
                    // Installation is not yet finished if this is a shared library
                    if (packageName == download?.packageName) onInstallationSuccess()
                }
                else -> {
                    val error = AppInstaller.getErrorString(
                        context,
                        returnCode
                    )

                    postError(packageName, error, extra)
                }
            }
            if (::serviceConnection.isInitialized) {
                context.unbindService(serviceConnection)
            }
        } catch (th: Throwable) {
            th.printStackTrace()
        }
    }

    override fun postError(packageName: String, error: String?, extra: String?) {
        try {
            super.postError(packageName, error, extra)
            if (::serviceConnection.isInitialized) {
                context.unbindService(serviceConnection)
            }
        } catch (th: Throwable) {
            th.printStackTrace()
        }
    }

    override fun getUri(file: File): Uri {
        val uri = FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.fileProvider",
            file
        )

        context.grantUriPermission(
            PRIVILEGED_EXTENSION_PACKAGE_NAME,
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )

        return uri
    }
}
