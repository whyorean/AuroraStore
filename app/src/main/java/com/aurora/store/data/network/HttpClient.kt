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

package com.aurora.store.data.network

import android.content.Context
import android.util.Log
import com.aurora.store.data.model.ProxyInfo
import com.aurora.store.util.Preferences
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HttpClient {

    private const val TAG = "HttpClient"

    @Provides
    @Singleton
    fun getPreferredClient(@ApplicationContext context: Context, gson: Gson): IProxyHttpClient {
        val proxyEnabled = Preferences.getBoolean(context, Preferences.PREFERENCE_PROXY_ENABLED)
        val proxyInfoString = Preferences.getString(context, Preferences.PREFERENCE_PROXY_INFO)

        return if (proxyEnabled && proxyInfoString.isNotBlank() && proxyInfoString != "{}") {
            val proxyInfo = gson.fromJson(proxyInfoString, ProxyInfo::class.java)

            if (proxyInfo != null) {
                OkHttpClient.setProxy(proxyInfo)
            } else {
                Log.e(TAG, "Proxy info is unavailable, using default client")
                OkHttpClient
            }
        } else {
            Log.i(TAG, "Proxy is disabled")
            OkHttpClient
        }
    }
}
