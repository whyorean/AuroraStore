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

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.store.data.room.update.Update
import com.aurora.store.util.AppUtil
import com.aurora.store.util.DownloadWorkerUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdatesViewModel @Inject constructor(
    private val appUtil: AppUtil,
    private val downloadWorkerUtil: DownloadWorkerUtil,
) : ViewModel() {
    private val TAG = UpdatesViewModel::class.java.simpleName

    var updateAllEnqueued: Boolean = false

    val downloadsList get() = downloadWorkerUtil.downloadsList
    val updates get() = appUtil.updates

    private val _fetchingUpdates = MutableStateFlow(false)
    val fetchingUpdates = _fetchingUpdates.asStateFlow()

    fun fetchUpdates() {
        _fetchingUpdates.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                appUtil.checkUpdates()
            } catch (exception: Exception) {
                Log.d(TAG, "Failed to get updates", exception)
            }
        }.invokeOnCompletion { _fetchingUpdates.value = false }
    }

    fun download(update: Update) {
        viewModelScope.launch { downloadWorkerUtil.enqueueUpdate(update) }
    }

    fun cancelDownload(packageName: String) {
        viewModelScope.launch { downloadWorkerUtil.cancelDownload(packageName) }
    }

    fun cancelAll() {
        viewModelScope.launch { downloadWorkerUtil.cancelAll(true) }
    }
}
