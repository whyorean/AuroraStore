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
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.aurora.store.util.Log
import java.io.File

@Deprecated("Deprecated in favour of SessionInstaller")
class NativeInstaller(context: Context) : InstallerBase(context) {

    override fun install(packageName: String, files: List<Any>) {
        if (isAlreadyQueued(packageName)) {
            Log.i("$packageName already queued")
        } else {
            Log.i("Received native install request for $packageName")
            files.map {
                when (it) {
                    is File -> it
                    is String -> File(it)
                    else -> {
                        throw Exception("Invalid data, expecting listOf() File or String")
                    }
                }
            }.forEach {
                xInstall(it)
            }
        }
    }

    private fun xInstall(file: File) {
        val intent: Intent

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent = Intent(Intent.ACTION_INSTALL_PACKAGE)
            intent.data = getUri(file)
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        } else {
            intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
        intent.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, context.packageName)
        context.startActivity(intent)
    }
}
