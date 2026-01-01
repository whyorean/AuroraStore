/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.viewmodel.blacklist

import android.content.Context
import android.content.pm.PackageInfo
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.extensions.TAG
import com.aurora.store.AuroraApp
import com.aurora.store.data.event.BusEvent
import com.aurora.store.data.helper.UpdateHelper
import com.aurora.store.data.providers.BlacklistProvider
import com.aurora.store.util.CertUtil
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.Preferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@HiltViewModel
class BlacklistViewModel @Inject constructor(
    private val json: Json,
    private val updateHelper: UpdateHelper,
    private val blacklistProvider: BlacklistProvider,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val isAuroraOnlyFilterEnabled =
        Preferences.getBoolean(context, Preferences.PREFERENCE_FILTER_AURORA_ONLY, false)
    private val isFDroidFilterEnabled =
        Preferences.getBoolean(context, Preferences.PREFERENCE_FILTER_FDROID, true)
    private val isExtendedUpdateEnabled =
        Preferences.getBoolean(context, Preferences.PREFERENCE_UPDATES_EXTENDED)

    private val packages = MutableStateFlow<List<PackageInfo>?>(null)
    private val _filteredPackages = MutableStateFlow<List<PackageInfo>?>(null)
    val filteredPackages = _filteredPackages.asStateFlow()

    val blacklist = mutableStateListOf<String>()

    init {
        blacklist.addAll(blacklistProvider.blacklist)
        fetchApps()
    }

    private fun fetchApps() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                packages.value = PackageUtil.getAllValidPackages(context).also { pkgList ->
                    _filteredPackages.value = pkgList
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch apps", exception)
            }
        }
    }

    fun search(query: String) {
        if (query.isNotBlank()) {
            _filteredPackages.value = packages.value!!
                .filter {
                    it.applicationInfo!!.loadLabel(context.packageManager).contains(query, true) ||
                        it.packageName.contains(query, true)
                }
        } else {
            _filteredPackages.value = packages.value
        }
    }

    fun isFiltered(packageInfo: PackageInfo): Boolean = when {
        !isExtendedUpdateEnabled && !packageInfo.applicationInfo!!.enabled -> true

        isAuroraOnlyFilterEnabled -> !CertUtil.isAuroraStoreApp(
            context,
            packageInfo.packageName
        )

        isFDroidFilterEnabled -> CertUtil.isFDroidApp(context, packageInfo.packageName)

        else -> false
    }

    fun blacklist(packageName: String) {
        blacklist.add(packageName)
        blacklistProvider.blacklist(packageName)
        AuroraApp.Companion.events.send(BusEvent.Blacklisted(packageName))
    }

    fun blacklistAll() {
        blacklistProvider.blacklist = packages.value!!.map { it.packageName }.toMutableSet()
        blacklist.apply {
            clear()
            addAll(blacklistProvider.blacklist)
        }
        viewModelScope.launch { updateHelper.deleteAllUpdates() }
    }

    fun whitelist(packageName: String) {
        blacklist.remove(packageName)
        blacklistProvider.whitelist(packageName)
    }

    fun whitelistAll() {
        blacklist.clear()
        blacklistProvider.blacklist = mutableSetOf()
    }

    fun importBlacklist(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use {
                    val importedSet = json.decodeFromString<MutableSet<String>>(
                        it.bufferedReader().readText()
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

    fun exportBlacklist(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(json.encodeToString(blacklistProvider.blacklist).encodeToByteArray())
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to export blacklist", exception)
            }
        }
    }
}
