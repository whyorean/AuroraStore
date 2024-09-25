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
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.data.models.PlayResponse
import com.aurora.gplayapi.helpers.AuthHelper
import com.aurora.gplayapi.helpers.AuthValidator
import com.aurora.gplayapi.network.IHttpClient
import com.aurora.store.R
import com.aurora.store.data.model.AccountType
import com.aurora.store.data.model.Auth
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
    private val gson: Gson,
    private val spoofProvider: SpoofProvider,
    private val httpClient: IHttpClient
) {

    private val TAG = AuthProvider::class.java.simpleName

    val dispenserURL: String?
        get() {
            val dispensers = Preferences.getStringSet(context, PREFERENCE_DISPENSER_URLS)
            return if (dispensers.isNotEmpty()) dispensers.random() else null
        }

    val authData: AuthData?
        get() {
            Log.i(TAG, "Loading saved AuthData")
            val rawAuth: String = Preferences.getString(context, PREFERENCE_AUTH_DATA)
            return if (rawAuth.isNotBlank()) {
                gson.fromJson(rawAuth, AuthData::class.java)
            } else {
                null
            }
        }

    val isAnonymous: Boolean
        get() = AccountProvider.getAccountType(context) == AccountType.ANONYMOUS

    private val properties: Properties
        get() {
            val currentProperties = if (spoofProvider.isDeviceSpoofEnabled()) {
                spoofProvider.getSpoofDeviceProperties()
            } else {
                NativeDeviceInfoProvider(context).getNativeDeviceProperties()
            }
            setVendingVersion(currentProperties)
            return currentProperties
        }

    private val locale: Locale
        get() = if (spoofProvider.isLocaleSpoofEnabled()) {
            spoofProvider.getSpoofLocale()
        } else {
            Locale.getDefault()
        }

    /**
     * Checks whether saved AuthData is valid or not
     */
    suspend fun isSavedAuthDataValid(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                AuthValidator(authData ?: return@withContext false)
                    .using(httpClient)
                    .isValid()
            } catch (exception: Exception) {
                false
            }
        }
    }

    /**
     * Builds [AuthData] for login using personal Google account
     * @param email E-mail ID
     * @param token AAS or Auth token
     * @param tokenType Type of the token, one from [AuthHelper.Token]
     * @return Result encapsulating [AuthData] or exception
     */
    suspend fun buildGoogleAuthData(
        email: String,
        token: String,
        tokenType: AuthHelper.Token
    ): Result<AuthData> {
        return withContext(Dispatchers.IO) {
            try {
                return@withContext Result.success(
                    AuthHelper.build(
                        email = email,
                        token = token,
                        tokenType = tokenType,
                        properties = properties,
                        locale = locale,
                    )
                )
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to generate Session", exception)
                return@withContext Result.failure(exception)
            }
        }
    }

    /**
     * Builds [AuthData] for login using one of the dispensers
     * @return Result encapsulating [AuthData] or exception
     */
    suspend fun buildAnonymousAuthData(): Result<AuthData> {
        return withContext(Dispatchers.IO) {
            try {
                val playResponse = httpClient.getAuth(dispenserURL!!).also {
                    if (!it.isSuccessful) throwError(it, context)
                }

                val auth = gson.fromJson(String(playResponse.responseBytes), Auth::class.java)
                return@withContext Result.success(
                    AuthHelper.build(
                        email = auth.email,
                        token = auth.auth,
                        tokenType = AuthHelper.Token.AUTH,
                        isAnonymous = true,
                        properties = properties,
                        locale = locale
                    )
                )
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to generate AuthData", exception)
                return@withContext Result.failure(exception)
            }
        }
    }

    /**
     * Saves given [AuthData]
     */
    fun saveAuthData(authData: AuthData) {
        Preferences.putString(context, PREFERENCE_AUTH_DATA, gson.toJson(authData))
    }

    /**
     * Removes saved [AuthData]
     */
    fun removeAuthData(context: Context) {
        Preferences.remove(context, PREFERENCE_AUTH_DATA)
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

    @Throws(Exception::class)
    private fun throwError(playResponse: PlayResponse, context: Context) {
        when (playResponse.code) {
            400 -> throw Exception(context.getString(R.string.bad_request))
            403 -> throw Exception(context.getString(R.string.access_denied_using_vpn))
            404 -> throw Exception(context.getString(R.string.server_unreachable))
            429 -> throw Exception(context.getString(R.string.login_rate_limited))
            503 -> throw Exception(context.getString(R.string.server_maintenance))
            else -> {
                if (playResponse.errorString.isNotBlank()) {
                    throw Exception(playResponse.errorString)
                } else {
                    throw Exception(
                        context.getString(
                            R.string.failed_generating_session,
                            playResponse.code
                        )
                    )
                }
            }
        }
    }
}
