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

package com.aurora.store.data.providers

import android.content.Context
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.store.data.SingletonHolder
import com.aurora.store.util.Log
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_AUTH_DATA
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.lang.reflect.Modifier

class AuthProvider private constructor(var context: Context) {

    companion object : SingletonHolder<AuthProvider, Context>(::AuthProvider)

    private var gson: Gson = GsonBuilder()
        .excludeFieldsWithModifiers(Modifier.TRANSIENT)
        .create()

    fun getAuthData(): AuthData {
        return getSavedAuthData()
    }

    private fun getSavedAuthData(): AuthData {
        Log.i("Loading saved AuthData")

        val rawAuth: String = Preferences.getString(context, PREFERENCE_AUTH_DATA)
        return if (rawAuth.isNotEmpty())
            gson.fromJson(rawAuth, AuthData::class.java)
        else
            AuthData("", "")
    }
}