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

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.*
import android.content.pm.PackageInstaller.SessionParams
import android.os.Build
import android.os.IBinder
import android.os.IInterface
import androidx.annotation.RequiresApi
import com.aurora.extensions.isOAndAbove
import com.aurora.extensions.isSAndAbove
import com.aurora.store.R
import com.aurora.store.data.model.Installer
import com.aurora.store.data.receiver.InstallerStatusReceiver
import com.aurora.store.data.room.download.Download
import com.aurora.store.util.Log
import com.aurora.store.util.PackageUtil.isSharedLibraryInstalled
import dev.rikka.tools.refine.Refine
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

@RequiresApi(Build.VERSION_CODES.O)
class ShizukuInstaller(context: Context) : InstallerBase(context) {

    companion object {
        const val SHIZUKU_PACKAGE_NAME = "moe.shizuku.privileged.api"

        fun getInstallerInfo(context: Context): Installer {
            return Installer(
                id = 5,
                title = context.getString(R.string.pref_install_mode_shizuku),
                subtitle = context.getString(R.string.shizuku_installer_subtitle),
                description = context.getString(R.string.shizuku_installer_desc)
            )
        }
    }

    // Taken from LSPatch (https://github.com/LSPosed/LSPatch)
    private fun IBinder.wrap() = ShizukuBinderWrapper(this)
    private fun IInterface.asShizukuBinder() = this.asBinder().wrap()

    private val iPackageManager: IPackageManager by lazy {
        IPackageManager.Stub.asInterface(SystemServiceHelper.getSystemService("package").wrap())
    }

    private val iPackageInstaller: IPackageInstaller by lazy {
        IPackageInstaller.Stub.asInterface(iPackageManager.packageInstaller.asShizukuBinder())
    }

    private val packageInstaller: PackageInstaller? by lazy {
        if (isSAndAbove()) {
            Refine.unsafeCast<PackageInstaller>(
                PackageInstallerHidden(iPackageInstaller, "com.android.vending", null, 0)
            )
        } else if (isOAndAbove()) {
            Refine.unsafeCast<PackageInstaller>(
                PackageInstallerHidden(iPackageInstaller, "com.android.vending", 0)
            )
        } else null
    }

    override fun install(download: Download) {
        if (isAlreadyQueued(download.packageName)) {
            Log.i("${download.packageName} already queued")
        } else {
            download.sharedLibs.forEach {
                // Shared library packages cannot be updated
                if (!isSharedLibraryInstalled(context, it.packageName, it.versionCode)) {
                    install(download.packageName, download.versionCode, it.packageName)
                }
            }
            install(download.packageName, download.versionCode)
        }
    }

    private fun install(packageName: String, versionCode: Int, sharedLibPkgName: String = "") {
        Log.i("Received session install request for ${sharedLibPkgName.ifBlank { packageName }}")

        val (sessionId, session) = kotlin.runCatching {
            val params = SessionParams(SessionParams.MODE_FULL_INSTALL)

            // Replace existing app (Updates)
            var flags = Refine
                .unsafeCast<PackageInstallerHidden.SessionParamsHidden>(params).installFlags
            flags = flags or PackageManagerHidden.INSTALL_REPLACE_EXISTING
            Refine.unsafeCast<PackageInstallerHidden.SessionParamsHidden>(params).installFlags =
                flags

            val sessionId = packageInstaller!!.createSession(params)
            val iSession = IPackageInstallerSession.Stub.asInterface(
                iPackageInstaller.openSession(sessionId).asShizukuBinder()
            )
            val session = Refine.unsafeCast<PackageInstaller.Session>(
                PackageInstallerHidden.SessionHidden(iSession)
            )

            sessionId to session
        }.getOrElse { ex ->
            ex.printStackTrace()
            postError(
                packageName,
                context.getString(R.string.installer_status_failure),
                context.getString(R.string.installer_shizuku_unavailable)
            )
            return
        }

        try {
            Log.i("Writing splits to session for ${sharedLibPkgName.ifBlank { packageName }}")
            getFiles(packageName, versionCode, sharedLibPkgName).forEach {
                it.inputStream().use { input ->
                    session.openWrite("${sharedLibPkgName.ifBlank { packageName }}_${System.currentTimeMillis()}", 0, -1).use { output ->
                        input.copyTo(output)
                        session.fsync(output)
                    }
                }
            }

            val callBackIntent = Intent(context, InstallerStatusReceiver::class.java).apply {
                action = InstallerStatusReceiver.ACTION_INSTALL_STATUS
                setPackage(context.packageName)
                addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                putExtra(PackageInstaller.EXTRA_PACKAGE_NAME, sharedLibPkgName.ifBlank { packageName })
            }
            val flags = if (isSAndAbove()) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                callBackIntent,
                flags
            )

            Log.i("Starting install session for $packageName")
            session.commit(pendingIntent.intentSender)
            session.close()
        } catch (exception: Exception) {
            session.abandon()
            removeFromInstallQueue(packageName)
            postError(packageName, exception.localizedMessage, exception.stackTraceToString())
        }
    }
}
