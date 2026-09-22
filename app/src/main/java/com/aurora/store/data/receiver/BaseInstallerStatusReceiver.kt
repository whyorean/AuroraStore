/*
 * SPDX-FileCopyrightText: 2021 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.EXTRA_SESSION_ID
import android.util.Log
import androidx.core.content.IntentCompat
import com.aurora.extensions.TAG
import com.aurora.extensions.runOnUiThread
import com.aurora.store.AuroraApp
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.data.installer.AppInstaller.Companion.ACTION_INSTALL_STATUS
import com.aurora.store.data.installer.AppInstaller.Companion.EXTRA_DISPLAY_NAME
import com.aurora.store.data.installer.AppInstaller.Companion.EXTRA_PACKAGE_NAME
import com.aurora.store.data.installer.AppInstaller.Companion.EXTRA_VERSION_CODE
import com.aurora.store.data.installer.base.InstallerBase
import com.aurora.store.data.model.DownloadStatus
import com.aurora.store.data.room.download.DownloadDao
import com.aurora.store.util.NotificationUtil
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.PathUtil
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_AUTO_DELETE
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface InstallerStatusEntryPoint {
    fun downloadDao(): DownloadDao
}

abstract class BaseInstallerStatusReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent?.action == ACTION_INSTALL_STATUS) {
            val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: return
            val displayName = intent.getStringExtra(EXTRA_DISPLAY_NAME) ?: packageName

            val versionCode = intent.getLongExtra(EXTRA_VERSION_CODE, -1)
            val sessionId = intent.getIntExtra(EXTRA_SESSION_ID, -1)
            val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
            val extra = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)

            Log.i(
                TAG,
                "$packageName ($versionCode) sessionId=$sessionId, status=$status, extra=$extra"
            )

            // If package was successfully installed, exit after notifying user and doing cleanup
            if (status == PackageInstaller.STATUS_SUCCESS) {
                // No post-install steps for shared libraries
                if (PackageUtil.isSharedLibrary(context, packageName)) return

                AuroraApp.enqueuedInstalls.remove(packageName)
                InstallerBase.notifyInstallation(context, displayName, packageName)

                if (Preferences.getBoolean(context, PREFERENCE_AUTO_DELETE)) {
                    PathUtil.getAppDownloadDir(context, packageName, versionCode)
                        .deleteRecursively()
                }

                return postStatus(status, packageName, extra, context)
            }

            if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
                parkAwaitingInstall(context, packageName)
                AuroraApp.events.send(InstallerEvent.PendingUserAction(packageName))
                doAppropriatePrompt(context, intent, sessionId)
            } else {
                AuroraApp.enqueuedInstalls.remove(packageName)
                notifyUser(context, packageName, displayName, status)

                postStatus(status, packageName, extra, context)
            }
        }
    }

    private fun parkAwaitingInstall(context: Context, packageName: String) {
        val downloadDao = EntryPointAccessors
            .fromApplication(context, InstallerStatusEntryPoint::class.java)
            .downloadDao()

        AuroraApp.scope.launch {
            runCatching {
                val existing = downloadDao.getDownload(packageName)
                // A stale session can report back while a newer version is mid-download; parking
                // the row then would clobber that download and free the serialization slot.
                if (existing.status != DownloadStatus.INSTALLED && !existing.isActive) {
                    downloadDao.updateStatus(packageName, DownloadStatus.AWAITING_INSTALL)
                }
            }.onFailure { Log.e(TAG, "Failed to park $packageName", it) }
        }
    }

    private fun notifyUser(
        context: Context,
        packageName: String,
        displayName: String,
        status: Int
    ) {
        NotificationUtil.notifyInstallFailed(
            context,
            packageName,
            displayName,
            InstallerBase.getErrorString(context, status)
        )
    }

    internal fun promptUser(context: Context, intent: Intent) {
        runOnUiThread {
            val launchIntent = IntentCompat.getParcelableExtra(
                intent,
                Intent.EXTRA_INTENT,
                Intent::class.java
            )

            if (launchIntent == null) {
                Log.w(TAG, "No launch intent found in the installation request.")
                return@runOnUiThread
            }

            launchIntent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            launchIntent.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, context.packageName)
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            if (!AuroraApp.isForeground) {
                Log.i(TAG, "Aurora is backgrounded, notifying instead of prompting")
                val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: return@runOnUiThread
                NotificationUtil.notifyInstallPrompt(
                    context = context,
                    packageName = packageName,
                    displayName = intent.getStringExtra(EXTRA_DISPLAY_NAME) ?: packageName,
                    confirmIntent = launchIntent
                )
                return@runOnUiThread
            }

            try {
                context.startActivity(launchIntent)
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to launch intent!", exception)
            }
        }
    }

    open fun doAppropriatePrompt(context: Context, intent: Intent, sessionId: Int) {
        promptUser(context, intent)
    }

    open fun postStatus(status: Int, packageName: String, extra: String?, context: Context) {
        val event = when (status) {
            PackageInstaller.STATUS_SUCCESS -> InstallerEvent.Installed(
                packageName = packageName
            )

            else -> InstallerEvent.Failed(
                packageName = packageName,
                error = InstallerBase.getErrorString(context, status),
                extra = extra
            )
        }

        AuroraApp.events.send(event)
    }
}
