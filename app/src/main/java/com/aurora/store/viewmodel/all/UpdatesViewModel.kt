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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.store.data.room.update.Update
import com.aurora.store.data.helper.UpdateHelper
import com.aurora.store.data.helper.DownloadHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdatesViewModel @Inject constructor(
    private val updateHelper: UpdateHelper,
    private val downloadHelper: DownloadHelper,
) : ViewModel() {

    var updateAllEnqueued: Boolean = false

    val downloadsList get() = downloadHelper.downloadsList
    val updates get() = updateHelper.updates

    val fetchingUpdates = updateHelper.isCheckingUpdates

    fun fetchUpdates() {
        updateHelper.checkUpdatesNow()
    }

    fun download(update: Update) {
        viewModelScope.launch { downloadHelper.enqueueUpdate(update) }
    }

    fun downloadAll() {
        viewModelScope.launch {
            updates.value?.forEach { downloadHelper.enqueueUpdate(it) }
        }
    }

    fun cancelDownload(packageName: String) {
        viewModelScope.launch { downloadHelper.cancelDownload(packageName) }
    }

    fun cancelAll() {
        viewModelScope.launch { downloadHelper.cancelAll(true) }
    }
}
