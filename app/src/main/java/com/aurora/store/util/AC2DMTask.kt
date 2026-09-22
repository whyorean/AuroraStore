/*
 * SPDX-FileCopyrightText: 2021 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.util

import com.aurora.store.data.network.HttpClient
import java.util.Locale
import javax.inject.Inject
import okhttp3.RequestBody.Companion.toRequestBody

class AC2DMTask @Inject constructor(private val httpClient: HttpClient) {

    @Throws(Exception::class)
    fun getAC2DMResponse(email: String, oAuthToken: String): Map<String, String> {
        val params: MutableMap<String, Any> = hashMapOf()
        params["lang"] = Locale.getDefault().toString().replace("_", "-")
        params["google_play_services_version"] = PLAY_SERVICES_VERSION_CODE
        params["sdk_version"] = BUILD_VERSION_SDK
        params["device_country"] = Locale.getDefault().country.lowercase(Locale.US)
        params["Email"] = email
        params["service"] = "ac2dm"
        params["get_accountid"] = 1
        params["ACCESS_TOKEN"] = 1
        params["callerPkg"] = "com.google.android.gms"
        params["add_account"] = 1
        params["Token"] = oAuthToken
        params["callerSig"] = "38918a453d07199354f8b19af05ec6562ced5788"
        params["droidguard_results"] = "null"

        val body = params.map { "${it.key}=${it.value}" }.joinToString(separator = "&")

        val header = mapOf(
            "app" to "com.google.android.gms",
            "User-Agent" to "",
            "Content-Type" to "application/x-www-form-urlencoded"
        )

        val response = httpClient.post(TOKEN_AUTH_URL, header, body.toRequestBody())

        return if (response.isSuccessful) {
            AC2DMUtil.parseResponse(String(response.responseBytes))
        } else {
            emptyMap()
        }
    }

    companion object {
        private const val TOKEN_AUTH_URL = "https://android.clients.google.com/auth"
        private const val BUILD_VERSION_SDK = 28
        private const val PLAY_SERVICES_VERSION_CODE = 19629032
    }
}
