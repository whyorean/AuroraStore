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
import android.content.Context
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.data.models.App
import com.aurora.store.State
import com.aurora.store.data.RequestState
import com.aurora.store.data.downloader.RequestGroupIdBuilder
import com.aurora.store.data.downloader.getGroupId
import com.aurora.store.data.event.BusEvent
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.data.model.UpdateFile
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.FetchGroup
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*

class UpdatesViewModel(application: Application) : BaseAppsViewModel(application) {

    private val TAG = UpdatesViewModel::class.java.simpleName

    var updateFileMap: MutableMap<Int, UpdateFile> = mutableMapOf()
    var liveUpdateData: MutableLiveData<MutableMap<Int, UpdateFile>> = MutableLiveData()
    var updateAllEnqueued: Boolean = false

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
                it.versionCode.toLong() > PackageInfoCompat.getLongVersionCode(packageInfo)
            } else {
                false
            }
        }.sortedBy { it.displayName.lowercase(Locale.getDefault()) }.also { apps ->
            updateFileMap.clear()

            apps.forEach {
                updateFileMap[it.getGroupId(getApplication<Application>().applicationContext)] = UpdateFile(it)
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
            is InstallerEvent.Cancelled -> {

            }
            is InstallerEvent.Failed -> {
                val packageName = event.packageName
                packageName?.let {
                    val groupIDsOfPackageName = RequestGroupIdBuilder.getGroupIDsForApp(getApplication<Application>().applicationContext, packageName.hashCode())
                    groupIDsOfPackageName.forEach {
                        updateDownload(it, null, true)
                    }
                }
            }
        }
    }

    fun updateState(id: Int, state: State) {
        updateFileMap[id]?.state = state
        liveUpdateData.postValue(updateFileMap)
    }

    fun updateDownload(
        id: Int,
        group: FetchGroup?,
        isCancelled: Boolean = false,
        isComplete: Boolean = false
    ) {
        when {
            isCancelled -> {
                updateFileMap[id]?.state = State.IDLE
                updateFileMap[id]?.group = null
            }
            isComplete -> {
                updateFileMap[id]?.state = State.COMPLETE
                updateFileMap[id]?.group = group
            }
            else -> {
                updateFileMap[id]?.state = State.PROGRESS
                updateFileMap[id]?.group = group
            }
        }

        liveUpdateData.postValue(updateFileMap)
    }

    @Synchronized
    fun install(context: Context, packageName: String, files: List<Download>) {
        if (files.all { File(it.file).exists() }) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    AppInstaller.getInstance(context).getPreferredInstaller().install(
                        packageName,
                        files.filter { it.file.endsWith(".apk") }.map { it.file }.toList()
                    )
                } catch (exception: Exception) {
                    Log.e(TAG, "Failed to install $packageName", exception)
                }
            }
        } else {
            Log.e(TAG, "Given files doesn't exists!")
        }
    }

    private fun updateListAndPost(packageName: String) {
        val groupIDsOfPackageName = RequestGroupIdBuilder.getGroupIDsForApp(getApplication<Application>().applicationContext, packageName.hashCode())
        groupIDsOfPackageName.forEach {
            //Remove from map
            updateFileMap.remove(it)
        }

        //Post new update list
        liveUpdateData.postValue(updateFileMap)
    }

    override fun onCleared() {
        EventBus.getDefault().unregister(this)
        super.onCleared()
    }
}
