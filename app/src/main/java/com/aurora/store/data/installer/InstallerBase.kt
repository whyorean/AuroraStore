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
import androidx.core.content.FileProvider
import com.aurora.store.BuildConfig
import java.io.File

abstract class InstallerBase(protected var context: Context) : IInstaller {

    private val enqueuedInstalls: MutableSet<String> = mutableSetOf()

    override fun clearQueue() {
        enqueuedInstalls.clear()
    }

    override fun isAlreadyQueued(packageName: String): Boolean {
        return enqueuedInstalls.contains(packageName)
    }

    override fun removeFromInstallQueue(packageName: String) {
        enqueuedInstalls.remove(packageName)
    }

    override fun uninstall(packageName: String) {
        val uri = Uri.fromParts("package", packageName, null)
        val intent = Intent().apply {
            data = uri
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            intent.action = Intent.ACTION_DELETE
        } else {
            intent.action = Intent.ACTION_UNINSTALL_PACKAGE
            intent.putExtra(Intent.EXTRA_RETURN_RESULT, true)
        }

        context.startActivity(intent)
    }

    open fun getUri(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.fileProvider",
            file
        )
    }
}