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
import com.aurora.store.data.installer.AppInstaller.Companion.EXTRA_DOWNLOAD
import com.aurora.store.data.model.InstallerInfo
import com.aurora.store.data.receiver.InstallerStatusReceiver
import com.aurora.store.data.room.download.Download
import com.aurora.store.util.Log
import com.aurora.store.util.PackageUtil.isSharedLibraryInstalled
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionInstaller @Inject constructor(
    @ApplicationContext context: Context
) : InstallerBase(context) {

    val currentSessionId: Int?
        get() = enqueuedSessions.firstOrNull()?.keys?.last()

    private val packageInstaller = context.packageManager.packageInstaller
    private val enqueuedSessions = mutableListOf<MutableMap<Int, String>>()

    val callback = object : PackageInstaller.SessionCallback() {
        override fun onCreated(sessionId: Int) {}

        override fun onBadgingChanged(sessionId: Int) {}

        override fun onActiveChanged(sessionId: Int, active: Boolean) {}

        override fun onProgressChanged(sessionId: Int, progress: Float) {}

        override fun onFinished(sessionId: Int, success: Boolean) {
            enqueuedSessions.find { it.containsKey(sessionId) }?.let { sessionMap ->
                sessionMap.remove(sessionId)

                // If this was a shared lib, proceed installing other libs or actual package
                if (sessionMap.isNotEmpty() && success) {
                    val nextSession = sessionMap.keys.first()
                    commitInstall(sessionMap.getValue(nextSession), nextSession)
                } else {
                    enqueuedSessions.remove(sessionMap)
                }
            }

            if (enqueuedSessions.isNotEmpty()) {
                enqueuedSessions.firstOrNull()?.let { sessionMap ->
                    val nextSession = sessionMap.keys.first()
                    commitInstall(sessionMap.getValue(nextSession), nextSession)
                }
            } else {
                // Nothing else in queue, unregister callback
                packageInstaller.unregisterSessionCallback(this)
            }
        }
    }

    companion object {

        fun getInstallerInfo(context: Context): InstallerInfo {
            return InstallerInfo(
                id = 0,
                title = context.getString(R.string.pref_install_mode_session),
                subtitle = context.getString(R.string.session_installer_subtitle),
                description = context.getString(R.string.session_installer_desc)
            )
        }
    }

    init {
        runOnUiThread { packageInstaller.registerSessionCallback(callback) }
    }

    override fun install(download: Download) {
        super.install(download)

        val sessionMap = enqueuedSessions.find { it.values.contains(download.packageName) }
        if (sessionMap != null) {
            Log.i("${download.packageName} already queued")
            commitInstall(sessionMap.getValue(sessionMap.keys.first()), sessionMap.keys.first())
        } else {
            Log.i("Received session install request for ${download.packageName}")
            val sessionIdMap = mutableMapOf<Int, String>()

            download.sharedLibs.forEach {
                // Shared library packages cannot be updated
                if (!isSharedLibraryInstalled(context, it.packageName, it.versionCode)) {
                    stageInstall(download.packageName, download.versionCode, it.packageName)?.let { sessionID ->
                        sessionIdMap[sessionID] = it.packageName
                    }
                }
            }

            stageInstall(download.packageName, download.versionCode)?.let { sessionID ->
                sessionIdMap[sessionID] = download.packageName
            }

            if (enqueuedSessions.isEmpty()) {
                enqueuedSessions.add(sessionIdMap)
                commitInstall(
                    sessionIdMap.getValue(sessionIdMap.keys.first()),
                    sessionIdMap.keys.first()
                )
            } else {
                enqueuedSessions.add(sessionIdMap)
            }
        }
    }

    private fun stageInstall(packageName: String, versionCode: Int, sharedLibPkgName: String = ""): Int? {
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

        return try {
            Log.i("Writing splits to session for $packageName")
            getFiles(packageName, versionCode, sharedLibPkgName).forEach {
                it.inputStream().use { input ->
                    session.openWrite("${sharedLibPkgName.ifBlank { packageName }}_${it.name}", 0, it.length()).use { output ->
                        input.copyTo(output)
                        session.fsync(output)
                    }
                }
            }
            sessionId
        } catch (exception: Exception) {
            session.abandon()
            removeFromInstallQueue(packageName)
            postError(packageName, exception.localizedMessage, exception.stackTraceToString())
            null
        }
    }

    private fun commitInstall(packageName: String, sessionId: Int) {
        Log.i("Starting install session for $packageName")
        val session = packageInstaller.openSession(sessionId)
        session.commit(getCallBackIntent(packageName, sessionId).intentSender)
        session.close()
    }

    private fun getCallBackIntent(packageName: String, sessionId: Int): PendingIntent {
        val callBackIntent = Intent(context, InstallerStatusReceiver::class.java).apply {
            action = InstallerStatusReceiver.ACTION_INSTALL_STATUS
            setPackage(context.packageName)
            putExtra(PackageInstaller.EXTRA_PACKAGE_NAME, packageName)
            putExtra(EXTRA_DOWNLOAD, download)
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        }
        val flags = if (isSAndAbove()) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        return PendingIntent.getBroadcast(context, sessionId, callBackIntent, flags)
    }
}
