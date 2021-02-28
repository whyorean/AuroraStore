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
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.data.models.App
import com.aurora.store.data.RequestState
import com.aurora.store.data.event.BusEvent
import com.aurora.extensions.flushAndAdd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class UpdatesViewModel(application: Application) : BaseAppsViewModel(application) {

    var selectedUpdates: MutableSet<String> = mutableSetOf()
    var isUpdating: Boolean = false

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
            appList.flushAndAdd(apps)
            liveData.postValue(appList.sortedBy { it.displayName })
            requestState = RequestState.Complete
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onEvent(event: BusEvent) {
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

    private fun updateListAndPost(packageName: String) {
        //Remove from selected updates
        selectedUpdates.remove(packageName)

        if (selectedUpdates.isEmpty())
            isUpdating = false

        //Remove from current update list
        val updatedList = appList.filter {
            it.packageName != packageName
        }.toList()

        appList.flushAndAdd(updatedList)

        //Post new update list
        liveData.postValue(appList.sortedBy { it.displayName })
    }

    override fun onCleared() {
        EventBus.getDefault().unregister(this)
        super.onCleared()
    }
}