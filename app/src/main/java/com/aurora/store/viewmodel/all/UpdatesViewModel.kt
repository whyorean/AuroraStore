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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.data.models.App
import com.aurora.store.State
import com.aurora.store.data.RequestState
import com.aurora.store.data.event.BusEvent
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.data.model.UpdateFile
import com.tonyodev.fetch2.FetchGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class UpdatesViewModel(application: Application) : BaseAppsViewModel(application) {

    var updateFileMap: MutableMap<Int, UpdateFile> = mutableMapOf()
    var liveUpdateData: MutableLiveData<MutableMap<Int, UpdateFile>> = MutableLiveData()

    init {
        EventBus.getDefault().register(this)

        requestState = RequestState.Init
        observe()
    }

    override fun observe() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val marketApps = getFilteredApps()
                checkUpdate(marketApps)
            } catch (e: Exception) {
                requestState = RequestState.Pending
            }
        }
    }

    private fun checkUpdate(subAppList: List<App>) {
        subAppList.filter {
            val packageInfo = packageInfoMap[it.packageName]
            if (packageInfo != null) {
                it.versionCode > packageInfo.versionCode
            } else {
                false
            }
        }.also { apps ->
            apps.forEach {
                updateFileMap[it.id] = UpdateFile(it)
            }

            liveUpdateData.postValue(updateFileMap)
            requestState = RequestState.Complete
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onBusEvent(event: BusEvent) {
        when (event) {
            is BusEvent.InstallEvent -> {
                updateListAndPost(event.packageName)
            }
            is BusEvent.UninstallEvent -> {
                updateListAndPost(event.packageName)
            }
            is BusEvent.Blacklisted -> {
                updateListAndPost(event.packageName)
            }
            else -> {

            }
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onInstallerEvent(event: InstallerEvent) {
        when (event) {
            is InstallerEvent.Success -> {

            }
            is InstallerEvent.Failed -> {
                val packageName = event.packageName
                packageName?.let {
                    updateDownload(packageName.hashCode(), null, true)
                }
            }
        }
    }

    fun updateState(id: Int, state: State) {
        updateFileMap[id]?.state = state
        liveUpdateData.postValue(updateFileMap)
    }

    fun updateDownload(id: Int, group: FetchGroup?, isCancelled: Boolean = false) {
        if (isCancelled) {
            updateFileMap[id]?.state = State.IDLE
            updateFileMap[id]?.group = null
        } else {
            updateFileMap[id]?.state = State.PROGRESS
            updateFileMap[id]?.group = group
        }

        liveUpdateData.postValue(updateFileMap)
    }

    private fun updateListAndPost(packageName: String) {
        //Remove from map
        updateFileMap.remove(packageName.hashCode())

        //Post new update list
        liveUpdateData.postValue(updateFileMap)
    }

    override fun onCleared() {
        EventBus.getDefault().unregister(this)
        super.onCleared()
    }
}