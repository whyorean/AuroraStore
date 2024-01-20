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
import android.net.Uri
import androidx.core.content.FileProvider
import com.aurora.store.AuroraApplication
import com.aurora.store.BuildConfig
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.util.Log
import org.greenrobot.eventbus.EventBus
import java.io.File

abstract class InstallerBase(protected var context: Context) : IInstaller {

    override fun clearQueue() {
        AuroraApplication.enqueuedInstalls.clear()
    }

    override fun isAlreadyQueued(packageName: String): Boolean {
        return AuroraApplication.enqueuedInstalls.contains(packageName)
    }

    override fun removeFromInstallQueue(packageName: String) {
        AuroraApplication.enqueuedInstalls.remove(packageName)
    }

    open fun postError(packageName: String, error: String?, extra: String?) {
        Log.e("Service Error :$error")

        val event = InstallerEvent.Failed(
            packageName,
            error,
            extra
        )

        EventBus.getDefault().post(event)
    }

    open fun getUri(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.fileProvider",
            file
        )
    }
}
