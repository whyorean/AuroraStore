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
import com.aurora.extensions.flushAndAdd
import com.aurora.store.data.RequestState
import com.aurora.store.data.event.BusEvent
import com.aurora.store.util.AppUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.Locale

class InstalledViewModel(application: Application) : BaseAppsViewModel(application) {

    init {
        EventBus.getDefault().register(this)
        requestState = RequestState.Init
    }

    override fun observe() {
        viewModelScope.launch(Dispatchers.IO) {
            requestState = try {
                appList = AppUtil.getFilteredInstalledApps(getApplication()).toMutableList()
                liveData.postValue(appList.sortedBy { it.displayName.lowercase(Locale.getDefault()) })
                RequestState.Complete
            } catch (e: Exception) {
                RequestState.Pending
            }
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
                observe()
            }

            else -> {

            }
        }
    }

    private fun updateListAndPost(packageName: String) {
        //Remove from current list
        val updatedList = appList.filter {
            it.packageName != packageName
        }.toList()

        appList.flushAndAdd(updatedList)

        //Post new update list
        liveData.postValue(appList.sortedBy { it.displayName.lowercase(Locale.getDefault()) })
    }

    override fun onCleared() {
        EventBus.getDefault().unregister(this)
        super.onCleared()
    }
}
