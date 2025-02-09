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
import android.content.pm.PackageInfo
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.store.data.providers.BlacklistProvider
import com.aurora.store.util.CertUtil
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.Preferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BlacklistViewModel @Inject constructor(
    private val blacklistProvider: BlacklistProvider,
    private val gson: Gson,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val TAG = BlacklistViewModel::class.java.simpleName

    private val isAuroraOnlyFilterEnabled =
        Preferences.getBoolean(context, Preferences.PREFERENCE_FILTER_AURORA_ONLY, false)
    private val isFDroidFilterEnabled =
        Preferences.getBoolean(context, Preferences.PREFERENCE_FILTER_FDROID, true)
    private val isExtendedUpdateEnabled =
        Preferences.getBoolean(context, Preferences.PREFERENCE_UPDATES_EXTENDED)

    private val _packages = MutableStateFlow<List<PackageInfo>?>(null)
    val packages = _packages.asStateFlow()

    val blacklist = mutableStateListOf<String>()

    init {
        blacklist.addAll(blacklistProvider.blacklist)
        fetchApps()
    }

    private fun fetchApps() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _packages.value = PackageUtil.getAllValidPackages(context)
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch apps", exception)
            }
        }
    }

    fun isFiltered(packageInfo: PackageInfo): Boolean {
        return when {
            !isExtendedUpdateEnabled && !packageInfo.applicationInfo!!.enabled -> true
            isAuroraOnlyFilterEnabled -> !CertUtil.isAuroraStoreApp(context, packageInfo.packageName)
            isFDroidFilterEnabled -> CertUtil.isFDroidApp(context, packageInfo.packageName)
            else -> false
        }
    }

    fun blacklist(packageName: String) {
        blacklist.add(packageName)
        blacklistProvider.blacklist(packageName)
    }

    fun blacklistAll() {
        blacklistProvider.blacklist = packages.value!!.map { it.packageName }.toMutableSet()
        blacklist.apply {
            clear()
            addAll(blacklistProvider.blacklist)
        }
    }

    fun whitelist(packageName: String) {
        blacklist.remove(packageName)
        blacklistProvider.whitelist(packageName)
    }

    fun whitelistAll() {
        blacklist.clear()
        blacklistProvider.blacklist = mutableSetOf()
    }

    fun importBlacklist(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use {
                    val importedSet: MutableSet<String> = gson.fromJson(
                        it.bufferedReader().readText(),
                        object : TypeToken<MutableSet<String?>?>() {}.type
                    )

                    val validImportedSet = importedSet
                        .filter { pkgName -> packages.value!!.any { it.packageName == pkgName } }
                    blacklistProvider.blacklist.addAll(validImportedSet)
                    blacklist.apply {
                        clear()
                        addAll(blacklistProvider.blacklist)
                    }
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to import blacklist", exception)
            }
        }
    }

    fun exportBlacklist(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(gson.toJson(blacklistProvider.blacklist).encodeToByteArray())
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to export blacklist", exception)
            }
        }
    }
}
