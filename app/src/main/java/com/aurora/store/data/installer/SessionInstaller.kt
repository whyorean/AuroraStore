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
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.PACKAGE_SOURCE_STORE
import android.content.pm.PackageInstaller.SessionParams
import android.content.pm.PackageManager
import android.os.Process
import com.aurora.extensions.isNAndAbove
import com.aurora.extensions.isOAndAbove
import com.aurora.extensions.isSAndAbove
import com.aurora.extensions.isTAndAbove
import com.aurora.extensions.isUAndAbove
import com.aurora.extensions.runOnUiThread
import com.aurora.store.R
import com.aurora.store.data.model.Installer
import com.aurora.store.data.receiver.InstallerStatusReceiver
import com.aurora.store.data.room.download.Download
import com.aurora.store.util.Log
import com.aurora.store.util.PackageUtil.isSharedLibraryInstalled
import kotlin.properties.Delegates

class SessionInstaller(context: Context) : InstallerBase(context) {

    var parentSessionId by Delegates.notNull<Int>()

    private val packageInstaller = context.packageManager.packageInstaller
    private val sessionIdMap = mutableMapOf<Int, String>()

    companion object {

        fun getInstallerInfo(context: Context): Installer {
            return Installer(
                id = 0,
                title = context.getString(R.string.pref_install_mode_session),
                subtitle = context.getString(R.string.session_installer_subtitle),
                description = context.getString(R.string.session_installer_desc)
            )
        }
    }

    override fun install(download: Download) {
        if (isAlreadyQueued(download.packageName)) {
            Log.i("${download.packageName} already queued")
        } else {
            Log.i("Received session install request for ${download.packageName}")

            val callback = object : PackageInstaller.SessionCallback() {
                override fun onCreated(sessionId: Int) {}

                override fun onBadgingChanged(sessionId: Int) {}

                override fun onActiveChanged(sessionId: Int, active: Boolean) {}

                override fun onProgressChanged(sessionId: Int, progress: Float) {}

                override fun onFinished(sessionId: Int, success: Boolean) {
                    if (sessionId in sessionIdMap.keys && success) {
                        sessionIdMap.remove(sessionId)
                        if (sessionIdMap.isNotEmpty()) {
                            val nextSession = sessionIdMap.keys.first()
                            commitInstall(sessionIdMap.getValue(nextSession), nextSession)
                        } else {
                            packageInstaller.unregisterSessionCallback(this)
                        }
                    }
                }
            }

            download.sharedLibs.forEach {
                // Shared library packages cannot be updated
                if (!isSharedLibraryInstalled(context, it.packageName, it.versionCode)) {
                    stageInstall(download.packageName, download.versionCode, it.packageName)
                }
            }
            stageInstall(download.packageName, download.versionCode)

            if (sessionIdMap.size > 1) {
                runOnUiThread { packageInstaller.registerSessionCallback(callback) }
            }

            commitInstall(
                sessionIdMap.getValue(sessionIdMap.keys.first()),
                sessionIdMap.keys.first()
            )
        }
    }

    private fun stageInstall(packageName: String, versionCode: Int, sharedLibPkgName: String = "") {
        val packageInstaller = context.packageManager.packageInstaller

        val sessionParams = SessionParams(SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(sharedLibPkgName.ifBlank { packageName })
            setInstallLocation(PackageInfo.INSTALL_LOCATION_AUTO)
            if (isOAndAbove()) {
                setInstallReason(PackageManager.INSTALL_REASON_USER)
            }
            if (isNAndAbove()) {
                setOriginatingUid(Process.myUid())
            }
            if (isSAndAbove()) {
                setRequireUserAction(SessionParams.USER_ACTION_NOT_REQUIRED)
            }
            if (isTAndAbove()) {
                setPackageSource(PACKAGE_SOURCE_STORE)
            }
            if (isUAndAbove()) {
                setInstallerPackageName(context.packageName)
                setRequestUpdateOwnership(true)
            }
        }
        val sessionId = packageInstaller.createSession(sessionParams)
        val session = packageInstaller.openSession(sessionId)

        try {
            Log.i("Writing splits to session for $packageName")
            getFiles(packageName, versionCode, sharedLibPkgName).forEach {
                it.inputStream().use { input ->
                    session.openWrite(sharedLibPkgName.ifBlank { packageName }, 0, it.length()).use { output ->
                        input.copyTo(output)
                        session.fsync(output)
                    }
                }
            }

            // Add session to list of staged sessions
            sessionIdMap[sessionId] = packageName
            if (sharedLibPkgName.isBlank()) parentSessionId = sessionId
        } catch (exception: Exception) {
            session.abandon()
            removeFromInstallQueue(packageName)
            postError(packageName, exception.localizedMessage, exception.stackTraceToString())
        }
    }

    private fun commitInstall(packageName: String, sessionId: Int) {
        Log.i("Starting install session for $packageName")
        val session = packageInstaller.openSession(sessionId)
        session.commit(getCallBackIntent(packageName).intentSender)
        session.close()
    }

    private fun getCallBackIntent(packageName: String): PendingIntent {
        val callBackIntent = Intent(context, InstallerStatusReceiver::class.java).apply {
            action = InstallerStatusReceiver.ACTION_INSTALL_STATUS
            setPackage(context.packageName)
            putExtra(PackageInstaller.EXTRA_PACKAGE_NAME, packageName)
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        }
        val flags = if (isSAndAbove()) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        return PendingIntent.getBroadcast(context, parentSessionId, callBackIntent, flags)
    }
}
