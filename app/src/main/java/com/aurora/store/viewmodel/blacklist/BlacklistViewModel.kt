/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.viewmodel.blacklist

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.extensions.TAG
import com.aurora.store.AuroraApp
import com.aurora.store.compose.ui.commons.SortFilterPrefKeys
import com.aurora.store.compose.ui.commons.SortFilterState
import com.aurora.store.compose.ui.commons.applyFilter
import com.aurora.store.compose.ui.commons.applySort
import com.aurora.store.compose.ui.commons.loadSortFilterState
import com.aurora.store.compose.ui.commons.save
import com.aurora.store.data.event.BusEvent
import com.aurora.store.data.helper.UpdateHelper
import com.aurora.store.data.model.BlacklistAppItem
import com.aurora.store.data.providers.BlacklistProvider
import com.aurora.store.util.CertUtil
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.Preferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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

    private val packages = MutableStateFlow<List<BlacklistAppItem>?>(null)
    private val searchQuery = MutableStateFlow("")

    private val _state = MutableStateFlow(loadSortFilterState(context, PREF_KEYS))
    val state = _state.asStateFlow()

    private val _installers = MutableStateFlow<Map<String, String>>(emptyMap())
    val installers = _installers.asStateFlow()

    val filteredPackages = combine(packages, _state, searchQuery) { all, state, query ->
        all?.applyFilter(state)?.applySort(state)?.let { filtered ->
            if (query.isBlank()) {
                filtered
            } else {
                filtered.filter {
                    it.displayName.contains(query, true) ||
                        it.packageName.contains(query, true)
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val blacklist = mutableStateListOf<String>()

    init {
        blacklist.addAll(blacklistProvider.blacklist)
        fetchApps()
    }

    private fun fetchApps() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val items = PackageUtil.getAllValidPackages(context)
                    .mapNotNull { it.toBlacklistAppItem() }
                packages.value = items
                _installers.value = items.mapNotNull { it.installer }
                    .toSet()
                    .associateWith { resolveInstallerLabel(it) }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch apps", exception)
            }
        }
    }

    private fun PackageInfo.toBlacklistAppItem(): BlacklistAppItem? {
        val icon = PackageUtil.getIconForPackage(context, packageName) ?: return null
        val appInfo = applicationInfo!!
        return BlacklistAppItem(
            packageName = packageName,
            displayName = appInfo.loadLabel(context.packageManager).toString(),
            versionName = versionName ?: "",
            versionCode = PackageInfoCompat.getLongVersionCode(this),
            icon = icon,
            isFiltered = isFiltered(this),
            firstInstallTime = firstInstallTime,
            lastUpdateTime = lastUpdateTime,
            sizeBytes = runCatching { File(appInfo.sourceDir).length() }.getOrDefault(0L),
            isSystem = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0,
            installer = PackageUtil.getInstallerPackageName(context, packageName)
        )
    }

    fun search(query: String) {
        searchQuery.value = query
    }

    fun updateState(newState: SortFilterState) {
        if (newState == _state.value) return
        _state.value = newState
        newState.save(context, PREF_KEYS)
    }

    private fun resolveInstallerLabel(packageName: String): String = try {
        val info = PackageUtil.getPackageInfo(context, packageName)
        info.applicationInfo?.loadLabel(context.packageManager)?.toString() ?: packageName
    } catch (_: Exception) {
        packageName
    }

    private fun isFiltered(packageInfo: PackageInfo): Boolean = when {
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
        val allPackages = packages.value ?: return
        blacklistProvider.blacklist = allPackages.map { it.packageName }.toMutableSet()
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
                        .filter { pkgName ->
                            packages.value?.any { it.packageName == pkgName } ==
                                true
                        }
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

    companion object {
        private val PREF_KEYS = SortFilterPrefKeys(
            sortBy = Preferences.PREFERENCE_BLACKLIST_SORT_BY,
            sortOrder = Preferences.PREFERENCE_BLACKLIST_SORT_ORDER,
            appTypes = Preferences.PREFERENCE_BLACKLIST_APP_TYPES,
            installer = Preferences.PREFERENCE_BLACKLIST_INSTALLER
        )
    }
}
