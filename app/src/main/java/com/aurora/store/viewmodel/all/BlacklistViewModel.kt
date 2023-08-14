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
import android.content.pm.PackageManager
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aurora.store.data.RequestState
import com.aurora.store.data.model.Black
import com.aurora.store.data.providers.BlacklistProvider
import com.aurora.store.util.PackageUtil
import com.aurora.store.viewmodel.BaseAndroidViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.util.Locale

class BlacklistViewModel(application: Application) : BaseAndroidViewModel(application) {

    private val packageManager: PackageManager = application.packageManager
    private val blacklistProvider: BlacklistProvider = BlacklistProvider.with(application)

    var blackList: MutableList<Black> = mutableListOf()
    var selected: MutableSet<String> = mutableSetOf()

    val liveData: MutableLiveData<List<Black>> = MutableLiveData()

    init {
        requestState = RequestState.Init
        selected = blacklistProvider.getBlackList()
        observe()
    }

    override fun observe() {
        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                try {
                    val packageInfoMap = PackageUtil.getPackageInfoMap(getApplication())
                    packageInfoMap.values
                        .filter {
                            it.packageName != null
                                    && it.versionName != null
                                    && it.applicationInfo != null
                        }
                        .forEach {
                            val black = Black(it.packageName).apply {
                                displayName = packageManager.getApplicationLabel(it.applicationInfo)
                                    .toString()
                                versionCode = PackageInfoCompat.getLongVersionCode(it)
                                versionName = it.versionName
                                drawable = packageManager.getApplicationIcon(packageName)
                            }
                            blackList.add(black)
                        }
                    liveData.postValue(blackList.sortedBy { it.displayName.lowercase(Locale.getDefault()) })
                    requestState = RequestState.Complete
                } catch (e: Exception) {
                    e.printStackTrace()
                    requestState = RequestState.Pending
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}
