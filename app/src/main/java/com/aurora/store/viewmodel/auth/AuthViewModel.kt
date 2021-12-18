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

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aurora.Constants
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.data.providers.DeviceInfoProvider
import com.aurora.gplayapi.helpers.AuthHelper
import com.aurora.gplayapi.helpers.AuthValidator
import com.aurora.store.AccountType
import com.aurora.store.data.AuthState
import com.aurora.store.data.RequestState
import com.aurora.store.data.model.InsecureAuth
import com.aurora.store.data.network.HttpClient
import com.aurora.store.data.providers.AccountProvider
import com.aurora.store.data.providers.NativeDeviceInfoProvider
import com.aurora.store.data.providers.SpoofProvider
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_AUTH_DATA
import com.aurora.store.viewmodel.BaseAndroidViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import nl.komponents.kovenant.task
import java.net.ConnectException
import java.net.UnknownHostException
import java.util.*

class AuthViewModel(application: Application) : BaseAndroidViewModel(application) {

    private val spoofProvider = SpoofProvider(getApplication())

    val liveData: MutableLiveData<AuthState> = MutableLiveData()

    init {
        requestState = RequestState.Init
    }

    override fun observe() {
        val signedIn = Preferences.getBoolean(getApplication(), Constants.ACCOUNT_SIGNED_IN)
        if (signedIn) {
            liveData.postValue(AuthState.Available)
            buildSavedAuthData()
        } else {
            liveData.postValue(AuthState.Unavailable)
        }
    }

    fun buildGoogleAuthData(email: String, aasToken: String) {
        updateStatus("Requesting new session")

        task {
            var properties = NativeDeviceInfoProvider(getApplication()).getNativeDeviceProperties()
            if (spoofProvider.isDeviceSpoofEnabled())
                properties = spoofProvider.getSpoofDeviceProperties()

            return@task AuthHelper.build(email, aasToken, properties)
        } success {
            verifyAndSaveAuth(it, AccountType.GOOGLE)
        } fail {
            updateStatus("Failed to generate Session")
        }
    }

    fun buildAnonymousAuthData() {
        val insecure = Preferences.getBoolean(
            getApplication(),
            Preferences.PREFERENCE_INSECURE_ANONYMOUS
        )

        if (insecure) {
            buildInSecureAnonymousAuthData()
        } else {
            buildSecureAnonymousAuthData()
        }
    }

    private fun buildSecureAnonymousAuthData() {
        updateStatus("Requesting new session")

        task {
            var properties = NativeDeviceInfoProvider(getApplication())
                .getNativeDeviceProperties()

            if (spoofProvider.isDeviceSpoofEnabled())
                properties = spoofProvider.getSpoofDeviceProperties()

            val playResponse = HttpClient
                .getPreferredClient()
                .postAuth(
                    Constants.URL_DISPENSER,
                    gson.toJson(properties).toByteArray()
                )

            if (playResponse.isSuccessful) {
                return@task gson.fromJson(
                    String(playResponse.responseBytes),
                    AuthData::class.java
                )
            } else {
                when (playResponse.code) {
                    404 -> throw Exception("Server unreachable")
                    429 -> throw Exception("Oops, You are rate limited")
                    else -> throw Exception(playResponse.errorString)
                }
            }
        } success {
            //Set AuthData as anonymous
            it.isAnonymous = true
            verifyAndSaveAuth(it, AccountType.ANONYMOUS)
        } fail {
            updateStatus(it.message.toString())
        }
    }

    fun buildInSecureAnonymousAuthData() {
        updateStatus("Requesting new session")

        task {
            var properties = NativeDeviceInfoProvider(getApplication())
                .getNativeDeviceProperties()

            if (spoofProvider.isDeviceSpoofEnabled())
                properties = spoofProvider.getSpoofDeviceProperties()

            val playResponse = HttpClient
                .getPreferredClient()
                .getAuth(
                    Constants.URL_DISPENSER
                )

            val insecureAuth: InsecureAuth

            if (playResponse.isSuccessful) {
                insecureAuth = gson.fromJson(
                    String(playResponse.responseBytes),
                    InsecureAuth::class.java
                )
            } else {
                when (playResponse.code) {
                    404 -> throw Exception("Server unreachable")
                    429 -> throw Exception("Oops, You are rate limited")
                    else -> throw Exception(playResponse.errorString)
                }
            }

            val deviceInfoProvider = DeviceInfoProvider(properties, Locale.getDefault().toString())

            AuthHelper.buildInsecure(
                insecureAuth.email,
                insecureAuth.auth,
                Locale.getDefault(),
                deviceInfoProvider
            )
        } success {
            //Set AuthData as anonymous
            it.isAnonymous = true
            verifyAndSaveAuth(it, AccountType.ANONYMOUS)
        } fail {
            updateStatus(it.message.toString())
        }
    }

    private fun buildSavedAuthData() {
        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                try {
                    //Load & validate saved AuthData
                    val savedAuthData = getSavedAuthData()

                    if (isValid(savedAuthData)) {
                        liveData.postValue(AuthState.Valid)
                        requestState = RequestState.Complete
                    } else {
                        //Generate and validate new auth
                        val type = AccountProvider.with(getApplication()).getAccountType()
                        when (type) {
                            AccountType.GOOGLE -> {
                                val email = Preferences.getString(
                                    getApplication(),
                                    Constants.ACCOUNT_EMAIL_PLAIN
                                )
                                val aasToken = Preferences.getString(
                                    getApplication(),
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
                    when (e) {
                        is UnknownHostException -> updateStatus("No network")
                        is ConnectException -> updateStatus("Could not connect to server")
                        else -> updateStatus("Unknown error")
                    }
                    requestState = RequestState.Pending
                }
            }
        }
    }

    private fun getSavedAuthData(): AuthData {
        val rawAuth: String = Preferences.getString(getApplication(), PREFERENCE_AUTH_DATA)
        return if (rawAuth.isNotBlank())
            gson.fromJson(rawAuth, AuthData::class.java)
        else
            AuthData("", "")
    }

    private fun isValid(authData: AuthData): Boolean {
        return try {
            AuthValidator(authData)
                .using(HttpClient.getPreferredClient())
                .isValid()
        } catch (e: Exception) {
            false
        }
    }

    private fun verifyAndSaveAuth(authData: AuthData, type: AccountType) {
        updateStatus("Verifying new session")

        if (spoofProvider.isLocaleSpoofEnabled()) {
            authData.locale = spoofProvider.getSpoofLocale()
        } else {
            authData.locale = Locale.getDefault()
        }

        if (authData.authToken.isNotEmpty() && authData.deviceConfigToken.isNotEmpty()) {
            configAuthPref(authData, type, true)
            liveData.postValue(AuthState.SignedIn)
            requestState = RequestState.Complete
        } else {
            configAuthPref(authData, type, false)
            liveData.postValue(AuthState.SignedOut)
            requestState = RequestState.Pending

            updateStatus("Failed to verify session")
        }
    }

    private fun configAuthPref(authData: AuthData, type: AccountType, signedIn: Boolean) {
        if (signedIn) {
            //Save Auth Data
            Preferences.putString(
                getApplication(),
                PREFERENCE_AUTH_DATA,
                gson.toJson(authData)
            )
        }

        //Save Auth Type
        Preferences.putString(
            getApplication(),
            Constants.ACCOUNT_TYPE,
            type.name  // ANONYMOUS OR GOOGLE
        )

        //Save Auth Status
        Preferences.putBoolean(getApplication(), Constants.ACCOUNT_SIGNED_IN, signedIn)
    }

    private fun updateStatus(status: String) {
        liveData.postValue(AuthState.Status(status))
    }
}