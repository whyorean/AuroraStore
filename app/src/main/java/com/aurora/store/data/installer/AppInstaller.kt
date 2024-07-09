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

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.pm.PackageInfoCompat
import com.aurora.extensions.isOAndAbove
import com.aurora.extensions.isPAndAbove
import com.aurora.store.R
import com.aurora.store.data.model.InstallerInfo
import com.aurora.store.util.NotificationUtil
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_INSTALLER_ID
import com.topjohnwu.superuser.Shell
import dagger.hilt.android.qualifiers.ApplicationContext
import rikka.shizuku.Shizuku
import rikka.sui.Sui
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppInstaller @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionInstaller: SessionInstaller,
    private val nativeInstaller: NativeInstaller,
    private val rootInstaller: RootInstaller,
    private val serviceInstaller: ServiceInstaller,
    private val amInstaller: AMInstaller,
    private val shizukuInstaller: ShizukuInstaller
) {

    companion object {
        const val ACTION_INSTALL_STATUS = "com.aurora.store.data.installer.AppInstaller.INSTALL_STATUS"

        const val EXTRA_PACKAGE_NAME = "com.aurora.store.data.installer.AppInstaller.EXTRA_PACKAGE_NAME"
        const val EXTRA_VERSION_CODE = "com.aurora.store.data.installer.AppInstaller.EXTRA_VERSION_CODE"
        const val EXTRA_DISPLAY_NAME = "com.aurora.store.data.installer.AppInstaller.EXTRA_DISPLAY_NAME"

        enum class Installer {
            SESSION,
            NATIVE,
            ROOT,
            SERVICE,
            AM,
            SHIZUKU
        }

        fun getCurrentInstaller(context: Context): Installer {
            return Installer.entries[Preferences.getInteger(context, PREFERENCE_INSTALLER_ID)]
        }

        fun getAvailableInstallersInfo(context: Context): List<InstallerInfo> {
            val installers = mutableListOf(
                SessionInstaller.getInstallerInfo(context),
                NativeInstaller.getInstallerInfo(context)
            )

            if (hasRootAccess()) {
                installers.add(RootInstaller.getInstallerInfo(context))
            }

            if (hasAuroraService(context)) {
                installers.add(ServiceInstaller.getInstallerInfo(context))
            }

            if (hasAppManager(context)) {
                installers.add(AMInstaller.getInstallerInfo(context))
            }

            if (isOAndAbove() && hasShizukuOrSui(context)) {
                installers.add(ShizukuInstaller.getInstallerInfo(context))
            }

            return installers
        }

        fun notifyInstallation(context: Context, displayName: String, packageName: String) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notification = NotificationUtil.getInstallNotification(context, displayName, packageName)
            notificationManager.notify(packageName.hashCode(), notification)
        }

        fun getErrorString(context: Context, status: Int): String {
            return when (status) {
                PackageInstaller.STATUS_FAILURE_ABORTED -> context.getString(R.string.installer_status_user_action)
                PackageInstaller.STATUS_FAILURE_BLOCKED -> context.getString(R.string.installer_status_failure_blocked)
                PackageInstaller.STATUS_FAILURE_CONFLICT -> context.getString(R.string.installer_status_failure_conflict)
                PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> context.getString(R.string.installer_status_failure_incompatible)
                PackageInstaller.STATUS_FAILURE_INVALID -> context.getString(R.string.installer_status_failure_invalid)
                PackageInstaller.STATUS_FAILURE_STORAGE -> context.getString(R.string.installer_status_failure_storage)
                else -> context.getString(R.string.installer_status_failure)
            }
        }

        fun hasRootAccess(): Boolean {
            return Shell.getShell().isRoot
        }

        fun hasAuroraService(context: Context): Boolean {
            return try {
                val packageInfo = PackageUtil.getPackageInfo(
                    context,
                    ServiceInstaller.PRIVILEGED_EXTENSION_PACKAGE_NAME
                )
                val version = PackageInfoCompat.getLongVersionCode(packageInfo)

                packageInfo.applicationInfo.enabled && version >= 9
            } catch (exception: Exception) {
                false
            }
        }

        fun hasAppManager(context: Context): Boolean {
            return PackageUtil.isInstalled(context, AMInstaller.AM_PACKAGE_NAME) or
                    PackageUtil.isInstalled(context, AMInstaller.AM_DEBUG_PACKAGE_NAME)
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun hasShizukuOrSui(context: Context): Boolean {
            return PackageUtil.isInstalled(
                context,
                ShizukuInstaller.SHIZUKU_PACKAGE_NAME
            ) || Sui.isSui()
        }

        fun hasShizukuPerm(): Boolean {
            return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }

        fun uninstall(context: Context, packageName: String) {
            val intent = Intent().apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (isPAndAbove()) {
                    action = Intent.ACTION_DELETE
                } else {
                    @Suppress("DEPRECATION")
                    action = Intent.ACTION_UNINSTALL_PACKAGE
                    putExtra(Intent.EXTRA_RETURN_RESULT, true)
                }
            }
            context.startActivity(intent)
        }
    }

    private val defaultInstaller: IInstaller
        get() = sessionInstaller

    fun getPreferredInstaller(): IInstaller {
        return when (getCurrentInstaller(context)) {
            Installer.SESSION -> sessionInstaller
            Installer.NATIVE -> nativeInstaller
            Installer.ROOT -> if (hasRootAccess()) rootInstaller else defaultInstaller
            Installer.SERVICE -> if (hasAuroraService(context)) serviceInstaller else defaultInstaller
            Installer.AM -> if (hasAppManager(context)) amInstaller else defaultInstaller
            Installer.SHIZUKU -> {
                if (isOAndAbove() && hasShizukuOrSui(context) && hasShizukuPerm()) {
                    shizukuInstaller
                } else {
                    defaultInstaller
                }
            }
        }
    }
}
