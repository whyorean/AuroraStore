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

package com.aurora.store.data.installer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.aurora.Constants.PACKAGE_NAME_PLAY_STORE
import com.aurora.store.BuildConfig
import com.aurora.store.R
import com.aurora.store.data.activity.MicroGInstallerActivity
import com.aurora.store.data.installer.base.InstallerBase
import com.aurora.store.data.model.Installer
import com.aurora.store.data.model.InstallerInfo
import com.aurora.store.data.room.download.Download
import com.aurora.store.util.PackageUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MicroGInstaller @Inject constructor(
    @ApplicationContext private val context: Context
) : InstallerBase(context) {

    companion object {

        val installerInfo: InstallerInfo
            get() = InstallerInfo(
                id = 6,
                installer = Installer.MICROG,
                packageNames = listOf(PACKAGE_NAME_PLAY_STORE),
                installerPackageNames = listOf(PACKAGE_NAME_PLAY_STORE),
                title = R.string.pref_install_mode_microg,
                subtitle = R.string.microg_installer_subtitle,
                description = R.string.microg_installer_desc
            )

        fun buildMicroGInstallIntent(
            context: Context,
            fileList: ArrayList<File>
        ): Intent {
            val uris = ArrayList<Uri>()
            fileList.forEach { file ->
                val uri = FileProvider.getUriForFile(
                    context,
                    "${BuildConfig.APPLICATION_ID}.fileProvider",
                    file
                )
                context.grantUriPermission(
                    PACKAGE_NAME_PLAY_STORE,
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                uris.add(uri)
            }

            return Intent("org.microg.vending.action.INSTALL_PACKAGE").apply {
                component = ComponentName(
                    PACKAGE_NAME_PLAY_STORE,
                    "org.microg.vending.installer.AppInstallActivity"
                )
                type = "application/vnd.android.package-archive"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addCategory(Intent.CATEGORY_DEFAULT)
            }
        }
    }

    private val TAG = MicroGInstaller::class.java.simpleName

    override fun install(download: Download) {
        super.install(download)

        when {
            isAlreadyQueued(download.packageName) -> {
                Log.i(TAG, "${download.packageName} already queued")
            }

            isMicroGInstallerAvailable(context) -> {
                Log.i(TAG, "Received microG install request for ${download.packageName}")

                val fileList = getFiles(download.packageName, download.versionCode)
                MicroGInstallerActivity.launch(context, download.packageName, fileList)

                Log.i(TAG, "Sent install request to microG installer for ${download.packageName}")
            }

            else -> {
                postError(
                    download.packageName,
                    context.getString(R.string.installer_status_failure),
                    context.getString(R.string.installer_microg_misconfigured)
                )
            }
        }
    }

    fun isMicroGInstallerAvailable(context: Context): Boolean {
        // TODO: Implement better checks to ensure its microg companion & correct version is installed
        return PackageUtil.isInstalled(context, PACKAGE_NAME_PLAY_STORE)
    }
}
