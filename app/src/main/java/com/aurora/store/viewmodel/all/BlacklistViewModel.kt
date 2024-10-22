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
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.extensions.isValidApp
import com.aurora.store.data.providers.BlacklistProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

@HiltViewModel
class BlacklistViewModel @Inject constructor(
    val blacklistProvider: BlacklistProvider,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val TAG = BlacklistViewModel::class.java.simpleName

    private val _packages = MutableStateFlow<List<PackageInfo>?>(null)
    val packages = _packages.asStateFlow()

    var selected: MutableSet<String> = blacklistProvider.blacklist

    init {
        fetchApps()
    }

    private fun fetchApps() {
        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                try {
                    _packages.value = context.packageManager.getInstalledPackages(0)
                        .filter { it.isValidApp(context.packageManager) }
                        .sortedBy {
                            it.applicationInfo!!.loadLabel(context.packageManager).toString()
                                .lowercase(Locale.getDefault())
                        }
                } catch (exception: Exception) {
                    Log.e(TAG, "Failed to fetch apps", exception)
                }
            }
        }
    }
}
