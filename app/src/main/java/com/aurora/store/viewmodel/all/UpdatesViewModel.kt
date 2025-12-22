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
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.aurora.store.data.helper.DownloadHelper
import com.aurora.store.data.helper.UpdateHelper
import com.aurora.store.data.paging.GenericPagingSource.Companion.pager
import com.aurora.store.data.room.update.Update
import com.aurora.store.data.room.update.UpdateWithDownload
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class UpdatesViewModel @Inject constructor(
    val updateHelper: UpdateHelper,
    private val downloadHelper: DownloadHelper
) : ViewModel() {

    private val _updates = MutableStateFlow<PagingData<UpdateWithDownload>>(PagingData.empty())
    val updates = _updates.asStateFlow()

    val isCheckingUpdates = updateHelper.isCheckingUpdates
    val hasOngoingUpdates = updateHelper.hasOngoingUpdates
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val updatesCount = updateHelper.updatesCount
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    init {
        getPagedUpdates()
    }

    fun fetchUpdates() {
        updateHelper.checkUpdatesNow()
    }

    fun download(update: Update) {
        viewModelScope.launch { downloadHelper.enqueueUpdate(update) }
    }

    fun downloadAll() {
        viewModelScope.launch {
            updateHelper.updates.value?.forEach { downloadHelper.enqueueUpdate(it) }
        }
    }

    fun cancelDownload(packageName: String) {
        viewModelScope.launch { downloadHelper.cancelDownload(packageName) }
    }

    fun cancelAll() {
        viewModelScope.launch { downloadHelper.cancelAll(true) }
    }

    private fun getPagedUpdates() {
        pager { updateHelper.pagedUpdates }.flow
            .distinctUntilChanged()
            .cachedIn(viewModelScope)
            .onEach { _updates.value = it }
            .launchIn(viewModelScope)
    }
}
