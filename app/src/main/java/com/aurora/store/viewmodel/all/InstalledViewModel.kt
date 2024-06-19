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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.data.models.App
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.util.AppUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class InstalledViewModel @Inject constructor(
    private val authProvider: AuthProvider
) : ViewModel() {

    private val TAG = InstalledViewModel::class.java.simpleName

    private var appList: MutableList<App> = mutableListOf()
    val liveData: MutableLiveData<List<App>> = MutableLiveData()

    fun getInstalledApps(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                appList = AppUtil.getFilteredInstalledApps(context, authProvider.authData).toMutableList()
                liveData.postValue(appList.sortedBy { it.displayName.lowercase(Locale.getDefault()) })
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to get installed apps", exception)
            }
        }
    }
}
