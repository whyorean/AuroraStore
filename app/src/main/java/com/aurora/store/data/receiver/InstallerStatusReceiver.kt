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
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.EXTRA_SESSION_ID
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.IntentCompat
import androidx.core.content.getSystemService
import com.aurora.Constants.PACKAGE_NAME_APP_GALLERY
import com.aurora.extensions.runOnUiThread
import com.aurora.store.AuroraApp
import com.aurora.store.R
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.data.installer.AppInstaller.Companion.ACTION_INSTALL_STATUS
import com.aurora.store.data.installer.AppInstaller.Companion.EXTRA_DISPLAY_NAME
import com.aurora.store.data.installer.AppInstaller.Companion.EXTRA_PACKAGE_NAME
import com.aurora.store.data.installer.AppInstaller.Companion.EXTRA_VERSION_CODE
import com.aurora.store.data.installer.base.InstallerBase
import com.aurora.store.util.CommonUtil.inForeground
import com.aurora.store.util.NotificationUtil
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.PathUtil
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_AUTO_DELETE
import com.huawei.appgallery.coreservice.api.ApiClient
import com.huawei.appgallery.coreservice.api.IConnectionResult
import com.huawei.appgallery.coreservice.api.PendingCall
import com.huawei.appgallery.coreservice.internal.framework.ipc.transport.data.BaseIPCRequest
import com.huawei.appgallery.coreservice.internal.framework.ipc.transport.data.BaseIPCResponse
import com.huawei.appmarket.framework.coreservice.Status
import com.huawei.appmarket.service.externalservice.distribution.thirdsilentinstall.SilentInstallRequest
import com.huawei.appmarket.service.externalservice.distribution.thirdsilentinstall.SilentInstallResponse
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InstallerStatusReceiver : BroadcastReceiver() {

    private val TAG = InstallerStatusReceiver::class.java.simpleName

    private lateinit var apiClient: ApiClient

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
                Log.i(
                    TAG,
                    "Pending user action for $packageName, sessionId=$sessionId, status=$status, extra=$extra"
                )

                if (isHuaweiSilentInstallSupported(context)) {
                    promptAppGallery(sessionId, context)
                } else {
                    if (inForeground()) {
                        promptUser(intent, context)
                    }
                }
            } else {
                AuroraApp.enqueuedInstalls.remove(packageName)
                notifyUser(context, packageName, displayName, status)

                postStatus(status, packageName, extra, context)
            }
        }
    }

    private fun notifyUser(
        context: Context,
        packageName: String,
        displayName: String,
        status: Int
    ) {
        val notificationManager = context.getSystemService<NotificationManager>()
        val notification = NotificationUtil.getInstallerStatusNotification(
            context,
            packageName,
            displayName,
            InstallerBase.getErrorString(context, status)
        )
        notificationManager!!.notify(packageName.hashCode(), notification)
    }

    private fun promptUser(intent: Intent, context: Context) {
        IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)?.let {
            it.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            it.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, context.packageName)
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            try {
                runOnUiThread { context.startActivity(it) }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to trigger installation!", exception)
            }
        }
    }

    private fun promptAppGallery(sessionId: Int, context: Context) {
        apiClient = ApiClient.Builder(context.applicationContext)
            .setHomeCountry("CN")
            .addConnectionCallbacks(object : ApiClient.ConnectionCallback {
                override fun onConnected() {
                    Log.i(TAG, "ApiClient connected")
                    requestSilentInstall(sessionId)
                }

                override fun onConnectionSuspended(cause: Int) {
                    Log.w(TAG, "ApiClient connection suspended: $cause")
                }

                override fun onConnectionFailed(result: IConnectionResult?) {
                    Log.e(TAG, "ApiClient failed to connect")
                    Log.e(TAG, result?.statusCode.toString())
                }
            })
            .build()

        apiClient.connect()
    }

    private fun requestSilentInstall(sessionId: Int) {
        val request = SilentInstallRequest().apply {
            setSessionId(sessionId)
        }

        val pendingResult = PendingCall<BaseIPCRequest, BaseIPCResponse>(
            apiClient,
            request
        )

        if (::apiClient.isInitialized && apiClient.isConnected) {
            pendingResult.setCallback { handleInstallStatus(it) }
        } else {
            Log.e(TAG, "ApiClient null or not connected")
        }
    }

    private fun handleInstallStatus(status: Status<BaseIPCResponse>) {
        val response = status.response
        val statusCode = status.statusCode

        Log.i(
            TAG,
            "[recv]---1--- statusCode: $statusCode, response: ${response?.javaClass ?: "null"}"
        )

        if (response is SilentInstallResponse) {
            if (statusCode == 0) {
                Log.i(TAG, "[recv]---2---  response: ${response.result}")
            }
        }

        if (response is BaseIPCResponse) {
            if (statusCode == 0) {
                Log.i(TAG, "[recv]---3---  response: $response")
            }
        }
    }

    private fun postStatus(status: Int, packageName: String?, extra: String?, context: Context) {
        val event = when (status) {
            PackageInstaller.STATUS_SUCCESS -> {
                InstallerEvent.Installed(packageName!!).apply {
                    this.extra = context.getString(R.string.installer_status_success)
                }
            }

            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                InstallerEvent.Cancelled(packageName!!).apply {
                    this.extra = InstallerBase.getErrorString(context, status)
                }
            }

            else -> {
                InstallerEvent.Failed(packageName!!).apply {
                    this.error = InstallerBase.getErrorString(context, status)
                    this.extra = extra ?: ""
                }
            }
        }

        AuroraApp.events.send(event)

        if (::apiClient.isInitialized && apiClient.isConnected) {
            apiClient.disconnect()
        }
    }

    private fun isHuaweiSilentInstallSupported(context: Context): Boolean {
        return try {
            val applicationInfo: ApplicationInfo = context.packageManager.getApplicationInfo(
                PACKAGE_NAME_APP_GALLERY,
                PackageManager.GET_META_DATA
            )

            val supportFunction = applicationInfo.metaData.getInt("appgallery_support_function")
            Log.i(TAG, "Huawei silent install support function: $supportFunction")

            (supportFunction and (1 shl 5)) != 0
        } catch (e: Exception) {
            false
        }
    }
}
