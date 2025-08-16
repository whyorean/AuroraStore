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

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import com.aurora.Constants.PACKAGE_NAME_APP_GALLERY
import com.huawei.appgallery.coreservice.api.ApiClient
import com.huawei.appgallery.coreservice.api.ApiCode
import com.huawei.appgallery.coreservice.api.IConnectionResult
import com.huawei.appgallery.coreservice.api.PendingCall
import com.huawei.appgallery.coreservice.internal.framework.ipc.transport.data.BaseIPCRequest
import com.huawei.appgallery.coreservice.internal.framework.ipc.transport.data.BaseIPCResponse
import com.huawei.appmarket.framework.coreservice.Status
import com.huawei.appmarket.service.externalservice.distribution.thirdsilentinstall.SilentInstallRequest
import com.huawei.appmarket.service.externalservice.distribution.thirdsilentinstall.SilentInstallResponse
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InstallerStatusReceiver : BaseInstallerStatusReceiver() {

    private val TAG = InstallerStatusReceiver::class.java.simpleName

    private lateinit var apiClient: ApiClient

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
    }

    override fun doAppropriatePrompt(context: Context, intent: Intent, sessionId: Int) {
        if (isHuaweiSilentInstallSupported(context)) {
            Log.i(
                TAG,
                "Huawei silent install supported, proceeding with ApiClient connection"
            )
            connectApiClient(context, intent, sessionId)
        } else {
            promptUser(context, intent)
        }
    }

    override fun postStatus(status: Int, packageName: String?, extra: String?, context: Context) {
        super.postStatus(status, packageName, extra, context)

        if (::apiClient.isInitialized && apiClient.isConnected) {
            apiClient.disconnect()
        }
    }

    private fun connectApiClient(context: Context, intent: Intent, sessionId: Int) {
        // Check if the ApiClient is already initialized and connected
        if (::apiClient.isInitialized && apiClient.isConnected) {
            Log.i(TAG, "ApiClient already connected, requesting silent install")
            requestSilentInstall(context, intent, sessionId)
            return
        }

        apiClient = ApiClient.Builder(context.applicationContext)
            .setHomeCountry("CN")
            .addConnectionCallbacks(object : ApiClient.ConnectionCallback {
                override fun onConnected() {
                    Log.i(TAG, "ApiClient connected")
                    requestSilentInstall(context, intent, sessionId)
                }

                override fun onConnectionSuspended(cause: Int) {
                    Log.w(TAG, "ApiClient connection suspended: $cause")
                }

                override fun onConnectionFailed(result: IConnectionResult?) {
                    Log.e(TAG, "ApiClient failed to connect with result: $result, prompting user")
                    promptUser(context, intent)
                }
            })
            .build()

        apiClient.connect()
    }

    private fun requestSilentInstall(context: Context, intent: Intent, sessionId: Int) {
        val request = SilentInstallRequest().apply {
            setSessionId(sessionId)
        }

        val pendingResult = PendingCall<BaseIPCRequest, BaseIPCResponse>(
            apiClient,
            request
        )

        if (::apiClient.isInitialized && apiClient.isConnected) {
            pendingResult.setCallback { handleIPCResponse(context, intent, it) }
        } else {
            Log.e(TAG, "ApiClient null or not connected")
            promptUser(context, intent)
        }
    }

    private fun handleIPCResponse(
        context: Context,
        intent: Intent,
        ipcResponse: Status<BaseIPCResponse>
    ) {
        val response = ipcResponse.response
        val statusCode = ipcResponse.statusCode

        when (response) {
            is SilentInstallResponse -> {
                Log.i(TAG, "IPC Response: ${ApiCode.getStatusCodeString(statusCode)}")

                if (statusCode != ApiCode.SUCCESS || response.result != ApiCode.SUCCESS) {
                    Log.e(TAG, "Silent install unavailable: $statusCode")
                    promptUser(context, intent)
                }
            }

            null -> {
                Log.e(TAG, "IPC response is null.")
                promptUser(context, intent)
            }

            else -> {
                Log.e(TAG, "Unexpected IPC response type: ${response.javaClass.name}")
                promptUser(context, intent)
            }
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
