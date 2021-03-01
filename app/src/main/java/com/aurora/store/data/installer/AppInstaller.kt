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

open class AppInstaller constructor(var context: Context) {

    companion object {
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
    }

    fun getPreferredInstaller(): IInstaller {
        val prefValue = Preferences.getInteger(
            context,
            PREFERENCE_INSTALLER_ID
        )

        return when (prefValue) {
            1 -> NativeInstaller(context)
            2 -> RootInstaller(context)
            3 -> ServiceInstaller(context)
            else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                SessionInstaller(context)
            } else {
                NativeInstaller(context)
            }
        }
    }
}