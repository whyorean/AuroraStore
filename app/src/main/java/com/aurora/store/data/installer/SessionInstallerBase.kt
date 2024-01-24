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

package com.aurora.store.data.installer

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import com.aurora.extensions.isSAndAbove
import com.aurora.store.data.receiver.InstallerStatusReceiver
import com.aurora.store.util.Log
import java.io.File

abstract class SessionInstallerBase(context: Context) : InstallerBase(context) {

    protected fun xInstall(
        sessionId: Int,
        session: PackageInstaller.Session,
        packageName: String,
        files: List<File>
    ) {

        try {
            Log.i("Writing splits to session for $packageName")

            files.forEach {
                it.inputStream().use { input ->
                    session.openWrite("${packageName}_${System.currentTimeMillis()}", 0, -1).use { output ->
                        input.copyTo(output)
                        session.fsync(output)
                    }
                }
            }

            val callBackIntent = Intent(context, InstallerStatusReceiver::class.java).apply {
                action = InstallerStatusReceiver.ACTION_INSTALL_STATUS
                setPackage(context.packageName)
                putExtra(PackageInstaller.EXTRA_PACKAGE_NAME, packageName)
                addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            }
            val flags = if (isSAndAbove())
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE else
                PendingIntent.FLAG_UPDATE_CURRENT

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                callBackIntent,
                flags
            )

            Log.i("Starting install session for $packageName")
            session.commit(pendingIntent.intentSender)
            session.close()
        } catch (e: Exception) {
            session.abandon()
            removeFromInstallQueue(packageName)

            postError(
                packageName,
                e.localizedMessage,
                e.stackTraceToString()
            )
        }
    }
}
