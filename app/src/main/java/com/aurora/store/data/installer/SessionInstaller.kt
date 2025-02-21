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
import android.content.pm.PackageInstaller.PreapprovalDetails
import android.content.pm.PackageInstaller.SessionParams
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.icu.util.ULocale
import android.os.Build
import android.os.Process
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.PendingIntentCompat
import com.aurora.extensions.isNAndAbove
import com.aurora.extensions.isOAndAbove
import com.aurora.extensions.isSAndAbove
import com.aurora.extensions.isTAndAbove
import com.aurora.extensions.isUAndAbove
import com.aurora.extensions.runOnUiThread
import com.aurora.gplayapi.data.models.App
import com.aurora.store.AuroraApp
import com.aurora.store.R
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.data.installer.AppInstaller.Companion.ACTION_INSTALL_PRE_APPROVE
import com.aurora.store.data.installer.AppInstaller.Companion.ACTION_INSTALL_STATUS
import com.aurora.store.data.installer.AppInstaller.Companion.EXTRA_DISPLAY_NAME
import com.aurora.store.data.installer.AppInstaller.Companion.EXTRA_PACKAGE_NAME
import com.aurora.store.data.installer.AppInstaller.Companion.EXTRA_VERSION_CODE
import com.aurora.store.data.installer.base.InstallerBase
import com.aurora.store.data.model.BuildType
import com.aurora.store.data.model.Installer
import com.aurora.store.data.model.InstallerInfo
import com.aurora.store.data.model.SessionInfo
import com.aurora.store.data.receiver.InstallerStatusReceiver
import com.aurora.store.data.room.download.Download
import com.aurora.store.util.PackageUtil.isSharedLibraryInstalled
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionInstaller @Inject constructor(
    @ApplicationContext private val context: Context
) : InstallerBase(context) {

    private val TAG = SessionInstaller::class.java.simpleName

    val currentSessionId: Int?
        get() = enqueuedSessions.firstOrNull()?.last()?.sessionId

    private val packageInstaller = context.packageManager.packageInstaller
    private val enqueuedSessions = mutableListOf<MutableSet<SessionInfo>>()

    val callback = object : PackageInstaller.SessionCallback() {
        override fun onCreated(sessionId: Int) {}

        override fun onBadgingChanged(sessionId: Int) {}

        override fun onActiveChanged(sessionId: Int, active: Boolean) {}

        override fun onProgressChanged(sessionId: Int, progress: Float) {
            val packageName = enqueuedSessions
                .find { set -> set.any { it.sessionId == sessionId } }
                ?.first()
                ?.packageName

            if (packageName != null && progress > 0.0) {
                AuroraApp.events.send(
                    InstallerEvent.Installing(packageName).apply {
                        this.progress = (progress * 100).toInt()
                    }
                )
            }
        }

        override fun onFinished(sessionId: Int, success: Boolean) {
            enqueuedSessions.find { set -> set.any { it.sessionId == sessionId } }
                ?.let { sessionSet ->
                    sessionSet.remove(sessionSet.first { it.sessionId == sessionId })

                    // If this was a shared lib, proceed installing other libs or actual package
                    if (sessionSet.isNotEmpty() && success) {
                        commitInstall(sessionSet.first()); return
                    } else {
                        enqueuedSessions.remove(sessionSet)
                    }
                }

            if (enqueuedSessions.isNotEmpty()) {
                enqueuedSessions.firstOrNull()?.let { sessionSet ->
                    commitInstall(sessionSet.first())
                }
            }
        }
    }

    companion object {

        val installerInfo: InstallerInfo
            get() = InstallerInfo(
                id = 0,
                installer = Installer.SESSION,
                packageNames = BuildType.PACKAGE_NAMES,
                installerPackageNames = BuildType.PACKAGE_NAMES,
                title = R.string.pref_install_mode_session,
                subtitle = R.string.session_installer_subtitle,
                description = R.string.session_installer_desc
            )
    }

    init {
        runOnUiThread { packageInstaller.registerSessionCallback(callback) }
    }

    override fun install(download: Download) {
        super.install(download)

        val sessionSet =
            enqueuedSessions.find { set -> set.any { it.packageName == download.packageName } }
        if (sessionSet != null) {
            Log.i(TAG, "${download.packageName} already queued")
            val sessionInfo = sessionSet.first()
            if (sessionInfo.preApproved) {
                preApprovedInstall(sessionInfo)
            } else {
                commitInstall(sessionInfo)
            }
        } else {
            Log.i(TAG, "Received session install request for ${download.packageName}")
            val sessionInfoSet = mutableSetOf<SessionInfo>()

            download.sharedLibs.forEach {
                // Shared library packages cannot be updated
                if (!isSharedLibraryInstalled(context, it.packageName, it.versionCode)) {
                    stageInstall(
                        download.packageName,
                        download.versionCode,
                        it.packageName
                    )?.let { sessionID ->
                        sessionInfoSet.add(SessionInfo(sessionID, it.packageName, it.versionCode))
                    }
                }
            }

            stageInstall(download.packageName, download.versionCode)?.let { sessionID ->
                sessionInfoSet.add(
                    SessionInfo(
                        sessionID,
                        download.packageName,
                        download.versionCode,
                        download.displayName
                    )
                )
            }

            // Enqueue and trigger installation
            enqueuedSessions.add(sessionInfoSet)
            commitInstall(sessionInfoSet.first())
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun preApprove(app: App, drawable: Drawable) {
        val sessionParams = buildSessionParams(app.packageName)

        val sessionId = packageInstaller.createSession(sessionParams)
        val session = packageInstaller.openSession(sessionId)

        val preapprovalDetailsBuilder = PreapprovalDetails.Builder().apply {
            setPackageName(app.packageName)
            setLabel(app.displayName)
            setLocale(ULocale.getDefault())
            setIcon(drawableToBitmap(drawable))
        }

        val preapprovalDetails = preapprovalDetailsBuilder.build()

        val sessionInfo = SessionInfo(
            sessionId,
            app.packageName,
            app.versionCode,
            app.displayName,
            true
        )

        val pendingIntent = getPreApprovalCallBackIntent(sessionInfo)

        session.requestUserPreapproval(
            preapprovalDetails,
            pendingIntent!!.intentSender
        )

        enqueuedSessions.add(
            mutableSetOf(
                sessionInfo
            )
        )
    }

    private fun stageInstall(
        packageName: String,
        versionCode: Int,
        sharedLibPkgName: String = ""
    ): Int? {
        val resolvedPackageName = sharedLibPkgName.ifBlank { packageName }

        val sessionParams = buildSessionParams(resolvedPackageName)
        val sessionId = packageInstaller.createSession(sessionParams)
        val session = packageInstaller.openSession(sessionId)

        return try {
            Log.i(TAG, "Writing splits to session for $packageName")
            getFiles(packageName, versionCode, sharedLibPkgName).forEach { file ->
                file.inputStream().use { input ->
                    session.openWrite(
                        "${resolvedPackageName}_${file.name}",
                        0,
                        file.length()
                    ).use { output ->
                        input.copyTo(output)
                        session.fsync(output)
                    }
                }
            }
            sessionId
        } catch (exception: IOException) {
            session.abandon()
            removeFromInstallQueue(packageName)
            postError(packageName, exception.localizedMessage, exception.stackTraceToString())
            null
        }
    }

    private fun preApprovedInstall(
        sessionInfo: SessionInfo
    ) {
        val session = packageInstaller.openSession(sessionInfo.sessionId)

        try {
            Log.i(TAG, "Writing splits to session for ${sessionInfo.packageName}")
            val files = getFiles(sessionInfo.packageName, sessionInfo.versionCode, "")
            files.forEach { file ->
                file.inputStream().use { input ->
                    session.openWrite(
                        "${sessionInfo.packageName}_${file.name}",
                        0,
                        file.length()
                    ).use { output ->
                        input.copyTo(output)
                        session.fsync(output)
                    }
                }
            }
            Log.i(TAG, "${files.size} files written to session for ${sessionInfo.packageName}")
            commitInstall(sessionInfo)
        } catch (exception: IOException) {
            session.abandon()
            removeFromInstallQueue(sessionInfo.packageName)
            postError(
                sessionInfo.packageName,
                exception.localizedMessage,
                exception.stackTraceToString()
            )
            throw exception
        }
    }

    private fun buildSessionParams(packageName: String): SessionParams {
        return SessionParams(SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(packageName)
            setInstallLocation(PackageInfo.INSTALL_LOCATION_AUTO)
            if (isNAndAbove) {
                setOriginatingUid(Process.myUid())
            }
            if (isOAndAbove) {
                setInstallReason(PackageManager.INSTALL_REASON_USER)
            }
            if (isSAndAbove) {
                setRequireUserAction(SessionParams.USER_ACTION_NOT_REQUIRED)
            }
            if (isTAndAbove) {
                setPackageSource(PACKAGE_SOURCE_STORE)
            }
            if (isUAndAbove) {
                setInstallerPackageName(context.packageName)
                setRequestUpdateOwnership(true)
                setApplicationEnabledSettingPersistent()
            }
        }
    }

    private fun commitInstall(sessionInfo: SessionInfo) {
        Log.i(TAG, "Starting install session for ${sessionInfo.packageName}")
        val session = packageInstaller.openSession(sessionInfo.sessionId)
        val intent = getCallBackIntent(sessionInfo)

        if (intent == null) {
            Log.e(TAG, "Failed to get callback intent for ${sessionInfo.packageName}")
            return
        }

        session.commit(intent.intentSender)
        session.close()
        Log.i(TAG, "Session committed for ${sessionInfo.packageName}")
    }

    private fun getCallBackIntent(sessionInfo: SessionInfo): PendingIntent? {
        val callBackIntent = Intent(context, InstallerStatusReceiver::class.java).apply {
            action = ACTION_INSTALL_STATUS
            setPackage(context.packageName)
            putExtra(EXTRA_PACKAGE_NAME, sessionInfo.packageName)
            putExtra(EXTRA_VERSION_CODE, sessionInfo.versionCode)
            putExtra(EXTRA_DISPLAY_NAME, sessionInfo.displayName)
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        }

        return PendingIntentCompat.getBroadcast(
            context,
            sessionInfo.sessionId,
            callBackIntent,
            PendingIntent.FLAG_UPDATE_CURRENT,
            true
        )
    }

    private fun getPreApprovalCallBackIntent(
        sessionInfo: SessionInfo
    ): PendingIntent? {
        val callBackIntent = Intent(context, InstallerStatusReceiver::class.java).apply {
            action = ACTION_INSTALL_PRE_APPROVE
            setPackage(context.packageName)
        }

        return PendingIntentCompat.getBroadcast(
            context,
            sessionInfo.sessionId,
            callBackIntent,
            PendingIntent.FLAG_UPDATE_CURRENT,
            true
        )
    }


    fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }

        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }
}
