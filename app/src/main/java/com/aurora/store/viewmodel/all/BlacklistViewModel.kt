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
import android.content.pm.PackageManager
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.extensions.isApp
import com.aurora.store.data.model.Black
import com.aurora.store.data.providers.BlacklistProvider
import com.aurora.store.util.PackageUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak") // false positive, see https://github.com/google/dagger/issues/3253
class BlacklistViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val packageManager: PackageManager = context.packageManager
    private val blacklistProvider: BlacklistProvider = BlacklistProvider.with(context)

    var blackList: MutableList<Black> = mutableListOf()
    var selected: MutableSet<String> = mutableSetOf()

    val liveData: MutableLiveData<List<Black>> = MutableLiveData()

    init {
        selected = blacklistProvider.getBlackList()
        observe()
    }

    fun observe() {
        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                try {
                    val packageInfoMap = PackageUtil.getPackageInfoMap(context)
                    packageInfoMap.values
                        .filter { it.isApp() }
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
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
