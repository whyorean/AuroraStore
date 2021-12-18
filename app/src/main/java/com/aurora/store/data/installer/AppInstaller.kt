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

import android.content.Context
import android.content.pm.PackageInstaller
import android.os.Build
import com.aurora.store.R
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_INSTALLER_ID

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
                val installer = RootInstaller(context)
                choiceAndInstaller[prefValue] = installer
                installer
            }
            3 -> {
                val installer = ServiceInstaller(context)
                choiceAndInstaller[prefValue] = installer
                installer
            }
            4 -> {
                val installer = AMInstaller(context)
                choiceAndInstaller[prefValue] = installer
                installer
            }
            else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val installer = SessionInstaller(context)
                choiceAndInstaller[prefValue] = installer
                installer
            } else {
                val installer = NativeInstaller(context)
                choiceAndInstaller[prefValue] = installer
                installer
            }
        }
    }
}