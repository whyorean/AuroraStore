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

import com.aurora.gplayapi.data.models.PlayResponse
import com.aurora.store.BuildConfig
import com.aurora.store.data.model.ProxyInfo
import com.aurora.store.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import okhttp3.Headers.Companion.toHeaders
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

object OkHttpClient : IProxyHttpClient {

    private const val POST = "POST"
    private const val GET = "GET"

    private val _responseCode = MutableStateFlow(100)
    override val responseCode: StateFlow<Int>
        get() = _responseCode.asStateFlow()

    private var okHttpClient = OkHttpClient()
    private val okHttpClientBuilder = OkHttpClient().newBuilder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .followRedirects(true)
        .followSslRedirects(true)

    override fun setProxy(proxyInfo: ProxyInfo): OkHttpClient {
        val proxy = Proxy(
            if (proxyInfo.protocol == "SOCKS") Proxy.Type.SOCKS else Proxy.Type.HTTP,
            InetSocketAddress.createUnresolved(proxyInfo.host, proxyInfo.port)
        )

        val proxyUser = proxyInfo.proxyUser
        val proxyPassword = proxyInfo.proxyPassword

        if (!proxyUser.isNullOrBlank() && !proxyPassword.isNullOrBlank()) {
            okHttpClientBuilder.proxyAuthenticator { _, response ->
                if (response.request.header("Proxy-Authorization") != null) {
                    return@proxyAuthenticator null
                }

                val credential = Credentials.basic(proxyUser, proxyPassword)
                response.request
                    .newBuilder()
                    .header("Proxy-Authorization", credential)
                    .build()
            }
        }

        okHttpClientBuilder.proxy(proxy)
        okHttpClient = okHttpClientBuilder.build()
        return this
    }

    @Throws(IOException::class)
    fun post(url: String, headers: Map<String, String>, requestBody: RequestBody): PlayResponse {
        val request = Request.Builder()
            .url(url)
            .headers(headers.toHeaders())
            .method(POST, requestBody)
            .build()
        return processRequest(request)
    }

    @Throws(IOException::class)
    override fun post(
        url: String,
        headers: Map<String, String>,
        params: Map<String, String>
    ): PlayResponse {
        val request = Request.Builder()
            .url(buildUrl(url, params))
            .headers(headers.toHeaders())
            .method(POST, "".toRequestBody(null))
            .build()
        return processRequest(request)
    }

    override fun postAuth(url: String, body: ByteArray): PlayResponse {
        val requestBody = body.toRequestBody("application/json".toMediaType(), 0, body.size)
        val request = Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "${BuildConfig.APPLICATION_ID}-${BuildConfig.VERSION_NAME}-${BuildConfig.VERSION_CODE}"
            )
            .method(POST, requestBody)
            .build()
        return processRequest(request)
    }

    @Throws(IOException::class)
    override fun post(url: String, headers: Map<String, String>, body: ByteArray): PlayResponse {
        return post(url, headers, body.toRequestBody())
    }

    @Throws(IOException::class)
    override fun get(url: String, headers: Map<String, String>): PlayResponse {
        return get(url, headers, mapOf())
    }

    @Throws(IOException::class)
    override fun get(
        url: String,
        headers: Map<String, String>,
        params: Map<String, String>
    ): PlayResponse {
        val request = Request.Builder()
            .url(buildUrl(url, params))
            .headers(headers.toHeaders())
            .method(GET, null)
            .build()
        return processRequest(request)
    }

    override fun getAuth(url: String): PlayResponse {
        val request = Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "${BuildConfig.APPLICATION_ID}-${BuildConfig.VERSION_NAME}-${BuildConfig.VERSION_CODE}"
            )
            .method(GET, null)
            .build()
        return processRequest(request)
    }

    @Throws(IOException::class)
    override fun get(
        url: String,
        headers: Map<String, String>,
        paramString: String
    ): PlayResponse {
        val request = Request.Builder()
            .url(url + paramString)
            .headers(headers.toHeaders())
            .method(GET, null)
            .build()
        return processRequest(request)
    }

    private fun processRequest(request: Request): PlayResponse {
        // Reset response code as flow doesn't sends the same value twice
        _responseCode.value = 0

        val call = okHttpClient.newCall(request)
        return buildPlayResponse(call.execute())
    }

    private fun buildUrl(url: String, params: Map<String, String>): HttpUrl {
        val urlBuilder = url.toHttpUrl().newBuilder()
        params.forEach {
            urlBuilder.addQueryParameter(it.key, it.value)
        }
        return urlBuilder.build()
    }

    private fun buildPlayResponse(response: Response): PlayResponse {
        return PlayResponse(
            isSuccessful = response.isSuccessful,
            code = response.code,
            responseBytes = response.body?.bytes() ?: byteArrayOf(),
            errorString = if (!response.isSuccessful) response.message else String()
        ).also {
            _responseCode.value = response.code
            Log.i("OKHTTP [${response.code}] ${response.request.url}")
        }
    }
}
