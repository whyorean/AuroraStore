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

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.store.data.providers.BlacklistProvider
import com.aurora.store.util.PackageUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class InstalledViewModel @Inject constructor(
    private val appDetailsHelper: AppDetailsHelper,
    private val blacklistProvider: BlacklistProvider,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val TAG = InstalledViewModel::class.java.simpleName

    private val _installedApps = MutableStateFlow<List<App>?>(null)
    val installedApps = _installedApps.asStateFlow()

    init {
        fetchApps()
    }

    fun fetchApps() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                PackageUtil.getPackageInfoMap(context).keys.let { packages ->
                    val filtersPackages = packages.filter { !blacklistProvider.isBlacklisted(it) }

                    _installedApps.value = appDetailsHelper.getAppByPackageName(filtersPackages)
                        .filter { it.displayName.isNotEmpty() }
                        .map { it.isInstalled = true; it }
                        .sortedBy { it.displayName.lowercase(Locale.getDefault()) }
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to get installed apps", exception)
            }
        }
    }
}
