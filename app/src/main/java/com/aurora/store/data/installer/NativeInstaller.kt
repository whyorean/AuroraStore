/*
 * SPDX-FileCopyrightText: 2021 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.installer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.aurora.extensions.TAG
import com.aurora.extensions.runOnUiThread
import com.aurora.store.R
import com.aurora.store.data.installer.base.InstallerBase
import com.aurora.store.data.model.BuildType
import com.aurora.store.data.model.Installer
import com.aurora.store.data.model.InstallerInfo
import com.aurora.store.data.room.download.Download
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Deprecated("Deprecated in favour of SessionInstaller")
class NativeInstaller @Inject constructor(
    @ApplicationContext private val context: Context
) : InstallerBase(context) {

    companion object {

        val installerInfo: InstallerInfo
            get() = InstallerInfo(
                id = 1,
                installer = Installer.NATIVE,
                installerPackageNames = BuildType.PACKAGE_NAMES,
                title = R.string.pref_install_mode_native,
                subtitle = R.string.native_installer_subtitle,
                description = R.string.native_installer_desc
            )
    }

    override fun install(download: Download) {
        if (isAlreadyQueued(download.packageName)) {
            Log.i(TAG, "${download.packageName} already queued")
        } else {
            Log.i(TAG, "Received native install request for ${download.packageName}")
            getFiles(download.packageName, download.versionCode).forEach { xInstall(it) }
        }
    }

    private fun xInstall(file: File) {
        val intent: Intent

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            @Suppress("DEPRECATION")
            intent = Intent(Intent.ACTION_INSTALL_PACKAGE)
            intent.data = getUri(file)
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        } else {
            intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
        intent.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, context.packageName)
        runOnUiThread { context.startActivity(intent) }
    }
}
