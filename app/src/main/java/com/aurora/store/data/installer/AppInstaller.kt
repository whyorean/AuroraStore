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
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import com.aurora.extensions.isOAndAbove
import com.aurora.extensions.isPAndAbove
import com.aurora.store.BuildConfig
import com.aurora.store.R
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_INSTALLER_ID
import com.topjohnwu.superuser.Shell
import rikka.shizuku.Shizuku
import rikka.sui.Sui

open class AppInstaller private constructor(var context: Context) {

    companion object {
        private var instance: AppInstaller? = null
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
        fun getInstance(context: Context): AppInstaller {
            if (instance == null) {
                instance = AppInstaller(context.applicationContext)
            }
            return instance!!
        }

        fun hasRootAccess(): Boolean {
            return Shell.getShell().isRoot
        }

        fun hasAuroraService(context: Context): Boolean {
            val isInstalled = PackageUtil.isInstalled(
                context,
                ServiceInstaller.PRIVILEGED_EXTENSION_PACKAGE_NAME
            )

            val isCorrectVersionInstalled =
                PackageUtil.isInstalled(
                    context,
                    ServiceInstaller.PRIVILEGED_EXTENSION_PACKAGE_NAME,
                    if (BuildConfig.VERSION_CODE < 31) 8 else 9
                )

            return isInstalled && isCorrectVersionInstalled
        }

        fun hasAppManager(context: Context): Boolean {
            return PackageUtil.isInstalled(context, AMInstaller.AM_PACKAGE_NAME) or
                    PackageUtil.isInstalled(context, AMInstaller.AM_DEBUG_PACKAGE_NAME)
        }

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

    val choiceAndInstaller = HashMap<Int, IInstaller>()

    fun getPreferredInstaller(): IInstaller {
        val prefValue = Preferences.getInteger(
            context,
            PREFERENCE_INSTALLER_ID
        )

        if (choiceAndInstaller.containsKey(prefValue)) {
            return choiceAndInstaller[prefValue]!!
        }

        return when (prefValue) {
            1 -> {
                val installer = NativeInstaller(context)
                choiceAndInstaller[prefValue] = installer
                installer
            }
            2 -> {
                val installer = if (hasRootAccess()) {
                    RootInstaller(context)
                } else {
                    SessionInstaller(context)
                }
                choiceAndInstaller[prefValue] = installer
                installer
            }
            3 -> {
                val installer = if (hasAuroraService(context)) {
                    ServiceInstaller(context)
                } else {
                    SessionInstaller(context)
                }
                choiceAndInstaller[prefValue] = installer
                installer
            }
            4 -> {
                val installer = if (hasAppManager(context)) {
                    AMInstaller(context)
                } else {
                    SessionInstaller(context)
                }
                choiceAndInstaller[prefValue] = installer
                installer
            }
            5 -> {
                if (isOAndAbove()) {
                    val installer = if (hasShizukuOrSui(context) && hasShizukuPerm()) {
                        ShizukuInstaller(context)
                    } else {
                        SessionInstaller(context)
                    }
                    choiceAndInstaller[prefValue] = installer
                    installer
                } else {
                    SessionInstaller(context)
                }
            }
            else -> {
                val installer = SessionInstaller(context)
                choiceAndInstaller[prefValue] = installer
                installer
            }
        }
    }
}
