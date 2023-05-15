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
import android.content.pm.PackageInstaller.PACKAGE_SOURCE_STORE
import android.content.pm.PackageInstaller.SessionParams
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import com.aurora.extensions.isMAndAbove
import com.aurora.extensions.isNAndAbove
import com.aurora.extensions.isSAndAbove
import com.aurora.extensions.isTAndAbove
import com.aurora.store.BuildConfig
import com.aurora.store.util.Log
import java.io.File

class SessionInstaller(context: Context) : InstallerBase(context) {

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun install(packageName: String, files: List<Any>) {
        if (isAlreadyQueued(packageName)) {
            Log.i("$packageName already queued")
        } else {
            Log.i("Received session install request for $packageName")
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
        val packageInstaller = context.packageManager.packageInstaller
        val sessionParams = SessionParams(SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(packageName)
            if (isNAndAbove()) {
                setOriginatingUid(android.os.Process.myUid())
            }
            if (isSAndAbove()) {
                setRequireUserAction(SessionParams.USER_ACTION_NOT_REQUIRED)
            }
            if (isTAndAbove()) {
                setPackageSource(PACKAGE_SOURCE_STORE)
            }
        }
        val sessionId = packageInstaller.createSession(sessionParams)
        val session = packageInstaller.openSession(sessionId)

        try {
            Log.i("Writing splits to session for $packageName")

            for (uri in uriList) {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    session.openWrite("${packageName}_${System.currentTimeMillis()}", 0, -1).use {
                        input.copyTo(it)
                        session.fsync(it)
                    }
                }
            }

            val callBackIntent = Intent(context, InstallerService::class.java)
            val flags = if (isSAndAbove())
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE else
                    PendingIntent.FLAG_UPDATE_CURRENT;
            val pendingIntent = PendingIntent.getService(
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

    override fun getUri(file: File): Uri {
        val uri = FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.fileProvider",
            file
        )

        context.grantUriPermission(
            BuildConfig.APPLICATION_ID,
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )

        return uri
    }
}