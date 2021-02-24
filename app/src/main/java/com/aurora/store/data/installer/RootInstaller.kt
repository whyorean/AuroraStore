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
import com.aurora.store.R
import com.aurora.store.util.Log
import com.aurora.extensions.isLAndAbove
import com.aurora.extensions.toast
import com.topjohnwu.superuser.Shell
import java.io.File
import java.util.regex.Pattern

class RootInstaller(context: Context) : InstallerBase(context) {

    override fun install(packageName: String, files: List<Any>) {
        if (isAlreadyQueued(packageName)) {
            Log.i("$packageName already queued")
        } else {
            if (Shell.getShell().isRoot) {
                files.map {
                    when (it) {
                        is File -> it
                        is String -> File(it)
                        else -> {
                            throw Exception("Invalid data, expecting listOf() File or String")
                        }
                    }
                }.let {
                    if (isLAndAbove())
                        xInstall(packageName, it)
                    else
                        xInstallLegacy(packageName, it)
                }
            } else {
                context.toast(context.getString(R.string.installer_root_unavailable))
                Log.e(" >>>>>>>>>>>>>>>>>>>>>>>>>> NO ROOT ACCESS <<<<<<<<<<<<<<<<<<<<<<<<<<<<<")
            }
        }
    }

    private fun xInstall(packageName: String, files: List<File>) {
        var totalSize = 0

        for (file in files)
            totalSize += file.length().toInt()

        val result: Shell.Result =
            Shell.su("pm install-create -i com.android.vending --user 0 -r -S $totalSize")
                .exec()

        val response = result.out

        val sessionIdPattern = Pattern.compile("(\\d+)")
        val sessionIdMatcher = sessionIdPattern.matcher(response[0])
        val found = sessionIdMatcher.find()

        if (found) {
            val sessionId = sessionIdMatcher.group(1).toInt()
            if (Shell.getShell().isRoot) {
                for (file in files) {
                    Shell.su("cat \"${file.absoluteFile}\" | pm install-write -S ${file.length()} $sessionId \"${file.name}\"")
                        .exec()
                }

                Shell.su("pm install-commit $sessionId").exec()
            } else {
                removeFromInstallQueue(packageName)
            }
        } else {
            removeFromInstallQueue(packageName)
        }
    }

    private fun xInstallLegacy(packageName: String, files: List<File>) {
        if (Shell.getShell().isRoot) {
            Shell.su("pm install -i com.android.vending --user 0 \"${files[0].name}\"").exec()
        } else {
            removeFromInstallQueue(packageName)
        }
    }
}