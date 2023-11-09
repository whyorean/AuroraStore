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
import android.content.pm.PackageInfo
import androidx.lifecycle.MutableLiveData
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.store.data.network.HttpClient
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.data.providers.BlacklistProvider
import com.aurora.store.util.PackageUtil
import com.aurora.store.viewmodel.BaseAndroidViewModel

abstract class BaseAppsViewModel(application: Application) : BaseAndroidViewModel(application) {

    private val authData = AuthProvider
        .with(application)
        .getAuthData()

    private val appDetailsHelper = AppDetailsHelper(authData)
        .using(HttpClient.getPreferredClient(application))

    var blacklistProvider = BlacklistProvider
        .with(application)

    val liveData: MutableLiveData<List<App>> = MutableLiveData()

    var appList: MutableList<App> = mutableListOf()
    var packageInfoMap: MutableMap<String, PackageInfo> = mutableMapOf()

    fun getFilteredApps(): List<App> {
        val blackList = blacklistProvider.getBlackList()

        packageInfoMap.clear()
        packageInfoMap = PackageUtil.getPackageInfoMap(getApplication())

        packageInfoMap.keys.let { packages ->
            /*Filter black list*/
            val filtersPackages = packages
                .filter { !blackList.contains(it) }

            return appDetailsHelper
                .getAppByPackageName(filtersPackages)
                .filter { it.displayName.isNotEmpty() }
                .map {
                    it.isInstalled = true
                    it
                }
        }
    }
}
