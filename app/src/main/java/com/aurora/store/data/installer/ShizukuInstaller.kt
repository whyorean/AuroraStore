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

import android.content.Context
import android.content.pm.*
import android.content.pm.PackageInstaller.SessionParams
import android.os.Build
import android.os.IBinder
import android.os.IInterface
import androidx.annotation.RequiresApi
import com.aurora.extensions.isOAndAbove
import com.aurora.extensions.isSAndAbove
import com.aurora.store.R
import com.aurora.store.data.room.download.Download
import com.aurora.store.util.Log
import dev.rikka.tools.refine.Refine
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

class ShizukuInstaller(context: Context) : SessionInstallerBase(context) {

    companion object {
        const val SHIZUKU_PACKAGE_NAME = "moe.shizuku.privileged.api"
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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun install(download: Download) {
        if (isAlreadyQueued(download.packageName)) {
            Log.i("${download.packageName} already queued")
        } else {
            Log.i("Received session install request for ${download.packageName}")

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
                    download.packageName,
                    context.getString(R.string.installer_status_failure),
                    context.getString(R.string.installer_shizuku_unavailable)
                )
                return
            }

            xInstall(
                sessionId,
                session,
                download.packageName,
                getFiles(download.packageName, download.versionCode)
            )
        }
    }
}
