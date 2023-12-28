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

package com.aurora.store.viewmodel.all

import android.app.Application
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.data.models.App
import com.aurora.store.util.DownloadWorkerUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

@HiltViewModel
class UpdatesViewModel @Inject constructor(
    application: Application,
    private val downloadWorkerUtil: DownloadWorkerUtil
) : BaseAppsViewModel(application) {

    private val TAG = UpdatesViewModel::class.java.simpleName

    var updateAllEnqueued: Boolean = false

    private val _updates = MutableSharedFlow<List<App>?>()
    val updates = _updates.asSharedFlow()

    val downloadsList get() = downloadWorkerUtil.downloadsList

    override fun observe() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getFilteredApps().filter {
                    val packageInfo = packageInfoMap[it.packageName]
                    if (packageInfo != null) {
                        it.versionCode.toLong() > PackageInfoCompat.getLongVersionCode(packageInfo)
                    } else {
                        false
                    }
                }.sortedBy { it.displayName.lowercase(Locale.getDefault()) }.also { apps ->
                    _updates.emit(apps)
                }
            } catch (exception: Exception) {
                Log.d(TAG, "Failed to get updates", exception)
            }
        }
    }

    fun download(app: App) {
        viewModelScope.launch { downloadWorkerUtil.enqueueApp(app) }
    }

    fun cancelDownload(app: App) {
        viewModelScope.launch { downloadWorkerUtil.cancelDownload(app.packageName) }
    }

    fun cancelAll() {
        viewModelScope.launch { downloadWorkerUtil.cancelAll() }
    }
}
