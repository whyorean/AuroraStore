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
import com.aurora.Constants
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.helpers.AuthValidator
import com.aurora.store.data.model.AccountType
import com.aurora.store.data.network.HttpClient
import com.aurora.store.util.Log
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_AUTH_DATA
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {

    val authData: AuthData? get() = getSavedAuthData()

    val isAnonymous: Boolean
        get() {
            val name = Preferences.getString(context, Constants.ACCOUNT_TYPE, AccountType.GOOGLE.name)
            return AccountType.valueOf(name) == AccountType.ANONYMOUS
        }

    suspend fun isSavedAuthDataValid(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                AuthValidator(authData ?: return@withContext false)
                    .using(HttpClient.getPreferredClient(context))
                    .isValid()
            } catch (exception: Exception) {
                false
            }
        }
    }

    private fun getSavedAuthData(): AuthData? {
        Log.i("Loading saved AuthData")
        val rawAuth: String = Preferences.getString(context, PREFERENCE_AUTH_DATA)
        return if (rawAuth.isNotEmpty()) {
            gson.fromJson(rawAuth, AuthData::class.java)
        } else {
            null
        }
    }
}
