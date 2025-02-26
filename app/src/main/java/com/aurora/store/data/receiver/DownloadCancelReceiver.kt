/*
 * SPDX-FileCopyrightText: 2025 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.aurora.store.AuroraApp
import com.aurora.store.data.helper.DownloadHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DownloadCancelReceiver : BroadcastReceiver() {
    private val TAG = DownloadCancelReceiver::class.java.simpleName

    @Inject
    lateinit var downloadHelper: DownloadHelper

    override fun onReceive(context: Context, intent: Intent?) {
        val packageName: String = intent?.getStringExtra("PACKAGE_NAME") ?: ""

        if (packageName.isNotBlank()) {
            Log.d(TAG, "Received cancel download request for $packageName")
            AuroraApp.scope.launch(Dispatchers.IO) {
                downloadHelper.cancelDownload(packageName)
            }
        }
    }
}
