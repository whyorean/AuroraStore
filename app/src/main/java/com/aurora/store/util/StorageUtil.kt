/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.util

import android.content.Context
import android.content.Intent
import android.os.storage.StorageManager
import android.provider.Settings
import android.util.Log
import androidx.core.content.getSystemService
import com.aurora.extensions.TAG
import com.aurora.extensions.isNMR1AndAbove
import com.aurora.extensions.isOAndAbove
import com.aurora.store.data.model.StorageRequirement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object StorageUtil {

    private const val BUFFER_BYTES = 64L * 1024 * 1024

    /**
     * Play reports only the download size, so the install copy is approximated as the same
     * again. Returns 0 for an unknown size, so a 0-byte app is never blocked on a guess.
     */
    fun requiredBytes(downloadSizes: List<Long>): Long {
        val total = downloadSizes.filter { it > 0 }.sumOf { it * 2 }
        return if (total > 0) total + BUFFER_BYTES else 0
    }

    /**
     * On Android O+ this is the allocatable size, which counts cache the system would evict,
     * matching what the worker later reserves via [StorageManager.allocateBytes].
     */
    suspend fun availableBytes(context: Context): Long = withContext(Dispatchers.IO) {
        val dir = PathUtil.getDownloadDirectory(context).apply { mkdirs() }
        if (isOAndAbove) {
            runCatching {
                val storageManager = context.getSystemService<StorageManager>()!!
                return@withContext storageManager
                    .getAllocatableBytes(storageManager.getUuidForPath(dir))
            }.onFailure { Log.w(TAG, "Failed to query allocatable bytes", it) }
        }
        dir.usableSpace
    }

    suspend fun check(
        context: Context,
        downloadSizes: List<Long>,
        appName: String? = null
    ): StorageRequirement = StorageRequirement(
        required = requiredBytes(downloadSizes),
        available = availableBytes(context),
        appName = appName,
        appCount = downloadSizes.size
    )

    fun openFreeUpSpace(context: Context) {
        val intent = if (isNMR1AndAbove) {
            Intent(StorageManager.ACTION_MANAGE_STORAGE)
        } else {
            Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
        }

        runCatching { context.startActivity(intent) }.recoverCatching {
            context.startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))
        }.onFailure { Log.e(TAG, "Failed to open storage settings", it) }
    }
}
