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
import android.util.Log
import com.aurora.Constants
import com.aurora.Constants.ACCOUNT_SIGNED_IN
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.helpers.AuthHelper
import com.aurora.gplayapi.helpers.AuthValidator
import com.aurora.store.R
import com.aurora.store.data.model.AccountType
import com.aurora.store.data.model.Auth
import com.aurora.store.data.network.HttpClient
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_AUTH_DATA
import com.aurora.store.util.Preferences.PREFERENCE_DISPENSER_URLS
import com.aurora.store.util.Preferences.PREFERENCE_VENDING_VERSION
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.Properties
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {

    private val TAG = AuthProvider::class.java.simpleName

    private val spoofProvider: SpoofProvider get() = SpoofProvider(context)
    val properties: Properties
        get() {
            val currentProperties = if (spoofProvider.isDeviceSpoofEnabled()) {
                spoofProvider.getSpoofDeviceProperties()
            } else {
                NativeDeviceInfoProvider(context).getNativeDeviceProperties()
            }
            setVendingVersion(currentProperties)
            return currentProperties
        }
    val locale: Locale
        get() = if (spoofProvider.isLocaleSpoofEnabled()) {
            spoofProvider.getSpoofLocale()
        } else {
            Locale.getDefault()
        }

    val dispenserURL: String?
        get() {
            val dispensers = Preferences.getStringSet(context, PREFERENCE_DISPENSER_URLS)
            return if (dispensers.isNotEmpty()) dispensers.random() else null
        }

    val authData: AuthData? get() = getSavedAuthData()

    val isAnonymous: Boolean
        get() {
            val name = Preferences.getString(context, Constants.ACCOUNT_TYPE, AccountType.GOOGLE.name)
            return AccountType.valueOf(name) == AccountType.ANONYMOUS
        }

    private val signedIn: Boolean
        get() = Preferences.getBoolean(context, ACCOUNT_SIGNED_IN)

    /**
     * Builds and returns AuthData based on signed-in account type
     */
    suspend fun getTmpAuthData(): AuthData? {
        return when {
            !signedIn -> null
            !isAnonymous -> buildGoogleAuthData()
            else -> buildAnonymousAuthData()
        }
    }

    /**
     * Checks whether saved AuthData is valid or not
     */
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
        Log.i(TAG, "Loading saved AuthData")
        val rawAuth: String = Preferences.getString(context, PREFERENCE_AUTH_DATA)
        return if (rawAuth.isNotEmpty()) {
            gson.fromJson(rawAuth, AuthData::class.java)
        } else {
            null
        }
    }

    private suspend fun buildGoogleAuthData(): AuthData? {
        return withContext(Dispatchers.IO) {
            try {
                val email = Preferences.getString(context, Constants.ACCOUNT_EMAIL_PLAIN)
                val aasToken = Preferences.getString(context, Constants.ACCOUNT_AAS_PLAIN)

                return@withContext AuthHelper.build(
                    email = email,
                    token = aasToken,
                    tokenType = AuthHelper.Token.AAS,
                    properties = properties,
                    locale = locale
                )
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to generate Session", exception)
                return@withContext null
            }
        }
    }

    private suspend fun buildAnonymousAuthData(): AuthData? {
        return withContext(Dispatchers.IO) {
            try {
                val playResponse = HttpClient
                    .getPreferredClient(context)
                    .getAuth(dispenserURL!!)

                val auth = gson.fromJson(String(playResponse.responseBytes), Auth::class.java)
                return@withContext AuthHelper.build(
                    email = auth.email,
                    token = auth.auth,
                    tokenType = AuthHelper.Token.AUTH,
                    isAnonymous = true,
                    properties = properties,
                    locale = locale
                )
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to generate AuthData", exception)
                return@withContext null
            }
        }
    }

    private fun setVendingVersion(currentProperties: Properties) {
        val vendingVersionIndex = Preferences.getInteger(context, PREFERENCE_VENDING_VERSION)
        if (vendingVersionIndex > 0) {
            val resources = context.resources
            val versionCodes = resources.getStringArray(R.array.pref_vending_version_codes)
            val versionStrings = resources.getStringArray(R.array.pref_vending_version)

            currentProperties.setProperty("Vending.version", versionCodes[vendingVersionIndex])
            currentProperties.setProperty("Vending.versionString", versionStrings[vendingVersionIndex])
        }
    }
}
