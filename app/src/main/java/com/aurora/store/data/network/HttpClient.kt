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

import android.util.Log
import com.aurora.extensions.TAG
import com.aurora.gplayapi.data.models.PlayResponse
import com.aurora.gplayapi.network.IHttpClient
import com.aurora.store.BuildConfig.APPLICATION_ID
import com.aurora.store.BuildConfig.VERSION_CODE
import com.aurora.store.BuildConfig.VERSION_NAME
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Headers.Companion.toHeaders
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

@Singleton
class HttpClient @Inject constructor(private val okHttpClient: OkHttpClient) : IHttpClient {

    companion object {
        private const val POST = "POST"
        private const val GET = "GET"
    }

    private val _responseCode = MutableStateFlow(100)
    override val responseCode: StateFlow<Int>
        get() = _responseCode.asStateFlow()

    @Throws(IOException::class)
    fun post(url: String, headers: Map<String, String>, requestBody: RequestBody): PlayResponse {
        val request = Request(
            url = url.toHttpUrl(),
            headers = headers.toHeaders(),
            method = POST,
            body = requestBody
        )
        return processRequest(request)
    }

    @Throws(IOException::class)
    fun call(url: String, headers: Map<String, String> = emptyMap()): Response {
        val request = Request(
            url = url.toHttpUrl(),
            headers = headers.toHeaders()
        )
        return okHttpClient.newCall(request).execute()
    }

    @Throws(IOException::class)
    override fun post(
        url: String,
        headers: Map<String, String>,
        params: Map<String, String>
    ): PlayResponse {
        val request = Request(
            url = buildUrl(url, params),
            headers = headers.toHeaders(),
            method = POST,
            body = "".toRequestBody(null)
        )
        return processRequest(request)
    }

    override fun postAuth(url: String, body: ByteArray): PlayResponse {
        val headers = mapOf("User-Agent" to "${APPLICATION_ID}-${VERSION_NAME}-${VERSION_CODE}")
        val requestBody = body.toRequestBody("application/json".toMediaType(), 0, body.size)
        val request = Request(
            url = url.toHttpUrl(),
            headers = headers.toHeaders(),
            method = POST,
            body = requestBody
        )
        return processRequest(request)
    }

    @Throws(IOException::class)
    override fun post(url: String, headers: Map<String, String>, body: ByteArray): PlayResponse =
        post(url, headers, body.toRequestBody())

    @Throws(IOException::class)
    override fun get(url: String, headers: Map<String, String>): PlayResponse =
        get(url, headers, mapOf())

    @Throws(IOException::class)
    override fun get(
        url: String,
        headers: Map<String, String>,
        params: Map<String, String>
    ): PlayResponse {
        val request = Request(
            url = buildUrl(url, params),
            headers = headers.toHeaders(),
            method = GET
        )
        return processRequest(request)
    }

    override fun getAuth(url: String): PlayResponse {
        val headers = mapOf("User-Agent" to "${APPLICATION_ID}-${VERSION_NAME}-${VERSION_CODE}")
        val request = Request(
            url = url.toHttpUrl(),
            headers = headers.toHeaders(),
            method = GET
        )
        return processRequest(request)
    }

    @Throws(IOException::class)
    override fun get(url: String, headers: Map<String, String>, paramString: String): PlayResponse {
        val request = Request(
            url = "$url$paramString".toHttpUrl(),
            headers = headers.toHeaders(),
            method = GET
        )
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

    private fun buildPlayResponse(response: Response): PlayResponse = PlayResponse(
        isSuccessful = response.isSuccessful,
        code = response.code,
        responseBytes = response.body.bytes(),
        errorString = if (!response.isSuccessful) response.message else String()
    ).also {
        val isCached = if (response.cacheResponse != null) "CACHED" else "NETWORK"
        _responseCode.value = response.code
        Log.i(TAG, "OKHTTP [$isCached] [${response.code}] ${response.request.url}")
    }
}
