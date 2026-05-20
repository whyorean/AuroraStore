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
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.aurora.extensions.isValidApp
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.helpers.web.WebAppDetailsHelper
import com.aurora.store.compose.ui.commons.InstalledAppMeta
import com.aurora.store.compose.ui.commons.SortFilterPrefKeys
import com.aurora.store.compose.ui.commons.SortFilterState
import com.aurora.store.compose.ui.commons.applyFilter
import com.aurora.store.compose.ui.commons.applySort
import com.aurora.store.compose.ui.commons.loadSortFilterState
import com.aurora.store.compose.ui.commons.save
import com.aurora.store.data.PageResult
import com.aurora.store.data.paging.GenericPagingSource
import com.aurora.store.data.paging.GenericPagingSource.Companion.manualPager
import com.aurora.store.data.providers.BlacklistProvider
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.Preferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@HiltViewModel
class InstalledViewModel @Inject constructor(
    blacklistProvider: BlacklistProvider,
    @ApplicationContext private val context: Context,
    private val webAppDetailsHelper: WebAppDetailsHelper
) : ViewModel() {

    private val blacklist = blacklistProvider.blacklist

    private val _apps = MutableStateFlow<PagingData<App>>(PagingData.empty())
    val apps = _apps.asStateFlow()

    private val _state = MutableStateFlow(loadSortFilterState(context, PREF_KEYS))
    val state = _state.asStateFlow()

    private val _installers = MutableStateFlow<Map<String, String>>(emptyMap())
    val installers = _installers.asStateFlow()

    private val _metadata = MutableStateFlow<Map<String, InstalledAppMeta>>(emptyMap())
    val metadata = _metadata.asStateFlow()

    /**
     * Enumerates installed packages and resolves per-package metadata used for sorting
     * and filtering. Done lazily on [Dispatchers.IO] so the VM constructor stays cheap;
     * subsequent state changes reuse this cached list.
     */
    private val enrichedPackages = viewModelScope.async(
        context = Dispatchers.IO,
        start = CoroutineStart.LAZY
    ) {
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(PackageManager.GET_META_DATA)
            .filter { it.isValidApp(pm) }
            .filterNot { it.packageName in blacklist }
            .map { info ->
                val appInfo = info.applicationInfo!!
                EnrichedPackage(
                    info = info,
                    label = appInfo.loadLabel(pm).toString(),
                    installer = PackageUtil.getInstallerPackageName(context, info.packageName),
                    sizeBytes = runCatching { File(appInfo.sourceDir).length() }.getOrDefault(0L),
                    isSystem = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
                )
            }

        // Build installer label map for the filter UI (packageName -> human label).
        _installers.value = packages.mapNotNull { it.installer }
            .toSet()
            .associateWith { resolveInstallerLabel(it) }

        // Per-app metadata lookup for the list item tertiary line.
        _metadata.value = packages.associateBy { it.packageName }

        packages
    }

    init {
        viewModelScope.launch {
            _state.collectLatest { state ->
                val all = enrichedPackages.await()
                val filteredSorted = all.applyFilter(state).applySort(state)
                val chunks = filteredSorted.chunked(GenericPagingSource.DEFAULT_PAGE_SIZE)
                manualPager(pageSize = GenericPagingSource.DEFAULT_PAGE_SIZE) { page ->
                    val chunk = chunks.getOrNull(page - 1)
                        ?: return@manualPager PageResult(emptyList<App>(), hasMore = false)
                    val items = webAppDetailsHelper.getAppDetails(chunk.map { it.packageName })
                    PageResult(items, hasMore = page < chunks.size)
                }.flow
                    .distinctUntilChanged()
                    .cachedIn(viewModelScope)
                    .collect { _apps.value = it }
            }
        }
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

    private data class EnrichedPackage(
        val info: PackageInfo,
        override val label: String,
        override val installer: String?,
        override val sizeBytes: Long,
        override val isSystem: Boolean
    ) : InstalledAppMeta {
        override val packageName: String get() = info.packageName
        override val firstInstallTime: Long get() = info.firstInstallTime
        override val lastUpdateTime: Long get() = info.lastUpdateTime
    }

    companion object {
        private val PREF_KEYS = SortFilterPrefKeys(
            sortBy = Preferences.PREFERENCE_INSTALLED_SORT_BY,
            sortOrder = Preferences.PREFERENCE_INSTALLED_SORT_ORDER,
            appTypes = Preferences.PREFERENCE_INSTALLED_APP_TYPES,
            installer = Preferences.PREFERENCE_INSTALLED_INSTALLER
        )
    }
}
