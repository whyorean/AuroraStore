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
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.aurora.extensions.TAG
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.helpers.web.WebAppDetailsHelper
import com.aurora.store.data.paging.GenericPagingSource.Companion.manualPager
import com.aurora.store.data.providers.BlacklistProvider
import com.aurora.store.util.PackageUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@HiltViewModel
class InstalledViewModel @Inject constructor(
    blacklistProvider: BlacklistProvider,
    @ApplicationContext private val context: Context,
    private val webAppDetailsHelper: WebAppDetailsHelper
) : ViewModel() {

    private val packages = PackageUtil.getAllValidPackages(context)
    private val blacklist = blacklistProvider.blacklist

    private val _apps = MutableStateFlow<PagingData<App>>(PagingData.empty())
    val apps = _apps.asStateFlow()

    init {
        fetchApps()
    }

    fun fetchApps() {
        val pagedPackages = packages
            .filterNot { it.packageName in blacklist }
            .chunked(20)

        manualPager { page ->
            try {
                webAppDetailsHelper.getAppDetails(
                    pagedPackages[page].map { it.packageName }
                )
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch apps", exception)
                emptyList()
            }
        }.flow.distinctUntilChanged()
            .cachedIn(viewModelScope)
            .onEach { _apps.value = it }
            .launchIn(viewModelScope)
    }
}
