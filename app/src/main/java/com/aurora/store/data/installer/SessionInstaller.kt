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
import android.content.pm.PackageInstaller.PACKAGE_SOURCE_STORE
import android.content.pm.PackageInstaller.SessionParams
import android.os.Build
import androidx.annotation.RequiresApi
import com.aurora.extensions.isNAndAbove
import com.aurora.extensions.isSAndAbove
import com.aurora.extensions.isTAndAbove
import com.aurora.store.util.Log


class SessionInstaller(context: Context) : SessionInstallerBase(context) {

    override fun install(packageName: String, files: List<Any>) {
        if (isAlreadyQueued(packageName)) {
            Log.i("$packageName already queued")
        } else {
            Log.i("Received session install request for $packageName")

            val packageInstaller = context.packageManager.packageInstaller
            val sessionParams = SessionParams(SessionParams.MODE_FULL_INSTALL).apply {
                setAppPackageName(packageName)
                if (isNAndAbove()) {
                    setOriginatingUid(android.os.Process.myUid())
                }
                if (isSAndAbove()) {
                    setRequireUserAction(SessionParams.USER_ACTION_NOT_REQUIRED)
                }
                if (isTAndAbove()) {
                    setPackageSource(PACKAGE_SOURCE_STORE)
                }
            }

            val sessionId = packageInstaller.createSession(sessionParams)
            val session = packageInstaller.openSession(sessionId)

            xInstall(sessionId, session, packageName, files)
        }
    }
}
