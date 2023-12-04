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
package com.aurora.store.data.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log
import androidx.core.content.IntentCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.aurora.Constants
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.util.NotificationUtil
import com.aurora.store.util.PathUtil
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import org.greenrobot.eventbus.EventBus
import kotlin.io.path.pathString

@AndroidEntryPoint
class InstallReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_INSTALL_APP = "com.aurora.store.data.receiver.InstallReceiver.INSTALL_APP"
        const val ACTION_INSTALL_STATUS =
            "com.aurora.store.data.receiver.InstallReceiver.INSTALL_STATUS"
    }

    private val TAG = InstallReceiver::class.java.simpleName

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_INSTALL_APP -> {
                val packageName = intent.extras?.getString(Constants.STRING_APP) ?: String()
                val version = intent.extras?.getInt(Constants.STRING_VERSION)
                if (packageName.isNotBlank() && version != null) {
                    try {
                        val downloadDir =
                            File(
                                PathUtil.getAppDownloadDir(
                                    context,
                                    packageName,
                                    version
                                ).pathString
                            )
                        AppInstaller.getInstance(context).getPreferredInstaller()
                            .install(
                                packageName,
                                downloadDir.listFiles()!!.filter { it.path.endsWith(".apk") }
                            )
                    } catch (exception: Exception) {
                        Log.e(TAG, "Failed to install $packageName")
                    }
                }
            }

            ACTION_INSTALL_STATUS -> {
                val packageName = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME)
                val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
                val extra = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)

                if (inForeground() && status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
                    promptUser(intent, context)
                } else {
                    postStatus(status, packageName, extra, context)
                    notifyInstallation(context, packageName!!, status)
                }
            }
        }
    }

    private fun notifyInstallation(context: Context, packageName: String, status: Int) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val content = if (status == PackageInstaller.STATUS_SUCCESS) {
            context.getString(R.string.installer_status_success)
        } else {
            AppInstaller.getErrorString(context, status)
        }
        val notification = NotificationUtil.getInstallNotification(
            context,
            App(packageName).apply {
                if (status == PackageInstaller.STATUS_SUCCESS) {
                    val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
                    displayName = context.packageManager.getApplicationLabel(appInfo).toString()
                }
            },
            content
        )
        notificationManager.notify(packageName.hashCode(), notification)
    }

    private fun promptUser(intent: Intent, context: Context) {
        val confirmationIntent =
            IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)

        confirmationIntent?.let {
            it.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            it.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, "com.android.vending")
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            try {
                context.startActivity(it)
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to trigger installation!", exception)
            }
        }
    }

    private fun postStatus(status: Int, packageName: String?, extra: String?, context: Context) {
        when (status) {
            PackageInstaller.STATUS_SUCCESS -> {
                EventBus.getDefault().post(
                    InstallerEvent.Success(
                        packageName,
                        context.getString(R.string.installer_status_success)
                    )
                )
            }

            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                EventBus.getDefault().post(
                    InstallerEvent.Cancelled(
                        packageName,
                        AppInstaller.getErrorString(context, status)
                    )
                )
            }

            else -> {
                EventBus.getDefault().post(
                    InstallerEvent.Failed(
                        packageName,
                        AppInstaller.getErrorString(context, status),
                        extra
                    )
                )
            }
        }
    }

    private fun inForeground(): Boolean {
        return ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)
    }
}
