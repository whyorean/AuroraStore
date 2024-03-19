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

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.extensions.flushAndAdd
import com.aurora.gplayapi.data.models.App
import com.aurora.store.data.event.BusEvent
import com.aurora.store.util.AppUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak") // false positive, see https://github.com/google/dagger/issues/3253
class InstalledViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val TAG = InstalledViewModel::class.java.simpleName

    var appList: MutableList<App> = mutableListOf()
    val liveData: MutableLiveData<List<App>> = MutableLiveData()

    init {
        EventBus.getDefault().register(this)
        observe()
    }

    fun observe() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                appList = AppUtil.getFilteredInstalledApps(context).toMutableList()
                liveData.postValue(appList.sortedBy { it.displayName.lowercase(Locale.getDefault()) })
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to get installed apps", exception)
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
