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
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.helpers.web.WebAppDetailsHelper
import com.aurora.store.data.providers.BlacklistProvider
import com.aurora.store.data.room.favourite.Favourite
import com.aurora.store.data.room.favourite.ImportExport
import com.aurora.store.util.PackageUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class InstalledViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val blacklistProvider: BlacklistProvider,
    private val json: Json,
    private val webAppDetailsHelper: WebAppDetailsHelper
) : ViewModel() {

    private val TAG = InstalledViewModel::class.java.simpleName

    private val _apps = MutableStateFlow<List<App>?>(null)
    val apps = _apps.asStateFlow()

    init {
        fetchApps()
    }

    fun fetchApps() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val packages = PackageUtil.getAllValidPackages(context)
                    .filterNot { blacklistProvider.isBlacklisted(it.packageName) }

                // Divide the list of packages into chunks of 100 & fetch app details
                // 50 is a safe number to avoid hitting the rate limit or package size limit
                val chunkedPackages = packages.chunked(50)
                val allApps = chunkedPackages.flatMap { chunk ->
                    webAppDetailsHelper.getAppDetails(chunk.map { it.packageName })
                }

                _apps.emit(allApps)
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch apps", exception)
            }
        }
    }

    fun exportApps(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val favourites: List<Favourite> = apps.value!!.map { app ->
                    Favourite.fromApp(app, Favourite.Mode.IMPORT)
                }
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(json.encodeToString(ImportExport(favourites)).encodeToByteArray())
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to installed apps", exception)
            }
        }
    }
}
