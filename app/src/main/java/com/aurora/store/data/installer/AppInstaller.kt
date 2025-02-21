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

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import com.aurora.extensions.getUpdateOwnerPackageNameCompat
import com.aurora.extensions.isOAndAbove
import com.aurora.extensions.isPAndAbove
import com.aurora.extensions.isSAndAbove
import com.aurora.store.BuildConfig
import com.aurora.store.data.installer.base.IInstaller
import com.aurora.store.data.model.Installer
import com.aurora.store.data.model.InstallerInfo
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
        const val ACTION_INSTALL_PRE_APPROVE = "com.aurora.store.data.installer.AppInstaller.INSTALL_PRE_APPROVE"

        const val EXTRA_PACKAGE_NAME = "com.aurora.store.data.installer.AppInstaller.EXTRA_PACKAGE_NAME"
        const val EXTRA_VERSION_CODE = "com.aurora.store.data.installer.AppInstaller.EXTRA_VERSION_CODE"
        const val EXTRA_DISPLAY_NAME = "com.aurora.store.data.installer.AppInstaller.EXTRA_DISPLAY_NAME"

        fun getCurrentInstaller(context: Context): Installer {
            return Installer.entries[Preferences.getInteger(context, PREFERENCE_INSTALLER_ID)]
        }

        fun getAvailableInstallersInfo(context: Context): List<InstallerInfo> {
            return listOfNotNull(
                SessionInstaller.installerInfo,
                NativeInstaller.installerInfo,
                if (hasRootAccess()) RootInstaller.installerInfo else null,
                if (hasAuroraService(context)) ServiceInstaller.installerInfo else null,
                if (hasAppManager(context)) AMInstaller.installerInfo else null,
                if (hasShizukuOrSui(context)) ShizukuInstaller.installerInfo else null
            )
        }

        /**
         * Checks if the given package can be silently installed
         * @param context [Context]
         * @param packageName Package to silently install
         */
        fun canInstallSilently(context: Context, packageName: String, targetSdk: Int): Boolean {
            return when (getCurrentInstaller(context)) {
                Installer.SESSION -> {
                    // Silent install cannot be done on initial install and below A12
                    if (!PackageUtil.isInstalled(context, packageName) || !isSAndAbove) return false

                    // We cannot do silent updates if we are not the update owner
                    if (context.packageManager.getUpdateOwnerPackageNameCompat(packageName) != BuildConfig.APPLICATION_ID) return false

                    // Ensure app being installed satisfies Android's requirement for targetSdk level
                    when (Build.VERSION.SDK_INT) {
                        Build.VERSION_CODES.VANILLA_ICE_CREAM -> targetSdk >= Build.VERSION_CODES.TIRAMISU
                        Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> targetSdk >= Build.VERSION_CODES.S
                        Build.VERSION_CODES.TIRAMISU -> targetSdk >= Build.VERSION_CODES.R
                        Build.VERSION_CODES.S -> targetSdk >= Build.VERSION_CODES.Q
                        else -> false // Only Android version above 12 can silently update apps
                    }
                }
                Installer.NATIVE -> false // Native installer requires user interaction
                Installer.ROOT -> hasRootAccess()
                Installer.SERVICE -> hasAuroraService(context)
                Installer.AM -> false // We cannot check if AppManager has ability to auto-update
                Installer.SHIZUKU -> isOAndAbove && hasShizukuOrSui(context) && hasShizukuPerm()
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

                packageInfo.applicationInfo!!.enabled && version >= 9
            } catch (exception: Exception) {
                false
            }
        }

        fun hasAppManager(context: Context): Boolean {
            return PackageUtil.isInstalled(context, AMInstaller.AM_PACKAGE_NAME) or
                    PackageUtil.isInstalled(context, AMInstaller.AM_DEBUG_PACKAGE_NAME)
        }

        fun hasShizukuOrSui(context: Context): Boolean {
            return isOAndAbove && (PackageUtil.isInstalled(
                context,
                ShizukuInstaller.SHIZUKU_PACKAGE_NAME
            ) || Sui.isSui())
        }

        fun hasShizukuPerm(): Boolean {
            return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }

        fun uninstall(context: Context, packageName: String) {
            val intent = Intent().apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (isPAndAbove) {
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
                if (hasShizukuOrSui(context) && hasShizukuPerm()) {
                    shizukuInstaller
                } else {
                    defaultInstaller
                }
            }
        }
    }
}
