/*
 * SPDX-FileCopyrightText: 2021 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.installer.base

import com.aurora.store.data.room.download.Download

interface IInstaller {
    fun install(download: Download)
    fun clearQueue()
    fun isAlreadyQueued(packageName: String): Boolean
    fun removeFromInstallQueue(packageName: String)

    /**
     * Abandons any staged-but-uncommitted install session for [packageName] so cancelling
     * a download doesn't leak a [android.content.pm.PackageInstaller] session. Default no-op
     * for installers that don't stage sessions.
     */
    fun cancelInstall(packageName: String) {}
}
