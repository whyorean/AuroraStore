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

package com.aurora.store.viewmodel.auth

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.Constants
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.data.models.PlayResponse
import com.aurora.gplayapi.helpers.AuthHelper
import com.aurora.store.AuroraApp
import com.aurora.store.R
import com.aurora.store.data.event.AuthEvent
import com.aurora.store.data.model.AccountType
import com.aurora.store.data.model.Auth
import com.aurora.store.data.model.AuthState
import com.aurora.store.data.network.HttpClient
import com.aurora.store.data.providers.AccountProvider
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.data.providers.NativeDeviceInfoProvider
import com.aurora.store.data.providers.SpoofProvider
import com.aurora.store.util.AC2DMTask
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_AUTH_DATA
import com.aurora.store.util.Preferences.PREFERENCE_DISPENSER_URLS
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.net.ConnectException
import java.net.UnknownHostException
import java.util.Locale
import java.util.Properties
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak") // false positive, see https://github.com/google/dagger/issues/3253
class AuthViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson,
    private val authProvider: AuthProvider
) : ViewModel() {

    private val TAG = AuthViewModel::class.java.simpleName

    private val spoofProvider: SpoofProvider get() = SpoofProvider(context)
    val properties: Properties
        get() = if (spoofProvider.isDeviceSpoofEnabled()) {
            spoofProvider.getSpoofDeviceProperties()
        } else {
            NativeDeviceInfoProvider(context).getNativeDeviceProperties()
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

    val liveData: MutableLiveData<AuthState> = MutableLiveData()

    fun observe() {
        if (liveData.value != AuthState.Fetching) {
            val signedIn = Preferences.getBoolean(context, Constants.ACCOUNT_SIGNED_IN)
            if (signedIn) {
                liveData.postValue(AuthState.Available)
                buildSavedAuthData()
            } else {
                liveData.postValue(AuthState.Unavailable)
            }
        }
    }

    fun buildGoogleAuthData(email: String, aasToken: String) {
        liveData.postValue(AuthState.Fetching)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val authData = AuthHelper.build(
                    email = email,
                    token = aasToken,
                    tokenType = AuthHelper.Token.AAS,
                    properties = properties,
                    locale = locale
                )
                verifyAndSaveAuth(authData, AccountType.GOOGLE)
            } catch (exception: Exception) {
                liveData.postValue(
                    AuthState.Failed(context.getString(R.string.failed_to_generate_session))
                )
                Log.e(TAG, "Failed to generate Session", exception)
            }
        }
    }

    fun buildAnonymousAuthData() {
        liveData.postValue(AuthState.Fetching)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val playResponse = HttpClient
                    .getPreferredClient(context)
                    .getAuth(dispenserURL!!)

                if (playResponse.isSuccessful) {
                    val tmpAuthData = gson.fromJson(
                        String(playResponse.responseBytes),
                        Auth::class.java
                    )

                    val authData = AuthHelper.build(
                        email = tmpAuthData.email,
                        token = tmpAuthData.auth,
                        tokenType = AuthHelper.Token.AUTH,
                        isAnonymous = true,
                        properties = properties,
                        locale = locale
                    )
                    verifyAndSaveAuth(authData, AccountType.ANONYMOUS)
                } else {
                    throwError(playResponse, context)
                }
            } catch (exception: Exception) {
                liveData.postValue(AuthState.Failed(exception.message.toString()))
                Log.e(TAG, "Failed to generate Session", exception)
            }
        }
    }

    fun buildAuthData(context: Context, email: String, oauthToken: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = AC2DMTask().getAC2DMResponse(email, oauthToken)
                if (response.isNotEmpty()) {
                    val aasToken = response["Token"]
                    if (aasToken != null) {
                        Preferences.putString(context, Constants.ACCOUNT_EMAIL_PLAIN, email)
                        Preferences.putString(context, Constants.ACCOUNT_AAS_PLAIN, aasToken)
                        AuroraApp.events.send(AuthEvent.GoogleLogin(true, email, aasToken))
                    } else {
                        Preferences.putString(context, Constants.ACCOUNT_EMAIL_PLAIN, "")
                        Preferences.putString(context, Constants.ACCOUNT_AAS_PLAIN, "")
                        AuroraApp.events.send(AuthEvent.GoogleLogin(false, "", ""))
                    }
                } else {
                    AuroraApp.events.send(AuthEvent.GoogleLogin(false, "", ""))
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to build AuthData", exception)
                AuroraApp.events.send(AuthEvent.GoogleLogin(false, "", ""))
            }
        }
    }

    private fun buildSavedAuthData() {
        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                try {
                    if (authProvider.isSavedAuthDataValid()) {
                        liveData.postValue(AuthState.Valid)
                    } else {
                        //Generate and validate new auth
                        val type = AccountProvider.with(context).getAccountType()
                        when (type) {
                            AccountType.GOOGLE -> {
                                val email = Preferences.getString(
                                    context,
                                    Constants.ACCOUNT_EMAIL_PLAIN
                                )
                                val aasToken = Preferences.getString(
                                    context,
                                    Constants.ACCOUNT_AAS_PLAIN
                                )
                                buildGoogleAuthData(email, aasToken)
                            }

                            AccountType.ANONYMOUS -> {
                                buildAnonymousAuthData()
                            }
                        }
                    }
                } catch (e: Exception) {
                    val error = when (e) {
                        is UnknownHostException -> {
                            context.getString(R.string.title_no_network)
                        }

                        is ConnectException -> {
                            context.getString(R.string.server_unreachable)
                        }

                        else -> {
                            context.getString(R.string.bad_request)
                        }
                    }
                    liveData.postValue(AuthState.Failed(error))
                }
            }
        }
    }

    private fun verifyAndSaveAuth(authData: AuthData, type: AccountType) {
        liveData.postValue(AuthState.Verifying)

        val versionId = Preferences.getInteger(context, Preferences.PREFERENCE_VENDING_VERSION)
        if (versionId > 0) {
            val resources = context.resources

            authData.deviceInfoProvider?.properties?.let {
                it.setProperty(
                    "Vending.version",
                    resources.getStringArray(R.array.pref_vending_version_codes)[versionId]
                )

                it.setProperty(
                    "Vending.versionString",
                    resources.getStringArray(R.array.pref_vending_version)[versionId]
                )
            }
        }

        if (authData.authToken.isNotEmpty() && authData.deviceConfigToken.isNotEmpty()) {
            configAuthPref(authData, type, true)
            liveData.postValue(AuthState.SignedIn)
        } else {
            configAuthPref(authData, type, false)
            liveData.postValue(AuthState.SignedOut)

            liveData.postValue(
                AuthState.Failed(context.getString(R.string.failed_to_generate_session))
            )
        }
    }

    private fun configAuthPref(authData: AuthData, type: AccountType, signedIn: Boolean) {
        if (signedIn) {
            //Save Auth Data
            Preferences.putString(context, PREFERENCE_AUTH_DATA, gson.toJson(authData))
        }

        //Save Auth Type
        Preferences.putString(context, Constants.ACCOUNT_TYPE, type.name)

        //Save Auth Status
        Preferences.putBoolean(context, Constants.ACCOUNT_SIGNED_IN, signedIn)
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
