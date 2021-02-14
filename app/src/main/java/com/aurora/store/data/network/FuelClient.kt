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

import com.aurora.gplayapi.GooglePlayApi
import com.aurora.gplayapi.data.models.PlayResponse
import com.aurora.gplayapi.network.IHttpClient
import com.aurora.store.BuildConfig
import com.aurora.store.util.Log
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.*
import java.nio.charset.Charset

object FuelClient : IHttpClient {

    override fun get(url: String, headers: Map<String, String>): PlayResponse {
        return get(url, headers, hashMapOf())
    }

    override fun get(
        url: String,
        headers: Map<String, String>,
        params: Map<String, String>
    ): PlayResponse {
        val parameters = params
            .map { it.key to it.value }
            .toList()
        val (request, response, result) = Fuel.get(url, parameters)
            .header(headers)
            .response()
        return buildPlayResponse(response, request)
    }

    override fun getAuth(url: String): PlayResponse {
        val (request, response, result) = Fuel.get(url)
            .appendHeader(
                "User-Agent",
                "${BuildConfig.APPLICATION_ID}-${BuildConfig.VERSION_NAME}-${BuildConfig.VERSION_CODE}"
            )
            .response()
        return buildPlayResponse(response, request)
    }

    override fun get(
        url: String,
        headers: Map<String, String>,
        paramString: String
    ): PlayResponse {
        val (request, response, result) = Fuel.get(url + paramString)
            .header(headers)
            .response()
        return buildPlayResponse(response, request)
    }

    override fun post(url: String, headers: Map<String, String>, body: ByteArray): PlayResponse {
        val (request, response, result) = Fuel.post(url)
            .header(headers)
            .appendHeader(Headers.CONTENT_TYPE, "application/x-protobuf")
            .body(body, Charset.defaultCharset())
            .response()
        return buildPlayResponse(response, request)
    }

    override fun post(
        url: String,
        headers: Map<String, String>,
        params: Map<String, String>
    ): PlayResponse {
        val parameters = params
            .map { it.key to it.value }
            .toList()
        val (request, response, result) = Fuel.post(url, parameters)
            .header(headers)
            .response()
        return buildPlayResponse(response, request)
    }

    override fun postAuth(url: String, body: ByteArray): PlayResponse {
        val (request, response, result) = Fuel.post(url)
            .appendHeader(
                "User-Agent",
                "${BuildConfig.APPLICATION_ID}-${BuildConfig.VERSION_NAME}-${BuildConfig.VERSION_CODE}"
            )
            .body(body)
            .response()
        return buildPlayResponse(response, request)
    }

    @JvmStatic
    private fun buildPlayResponse(response: Response, request: Request): PlayResponse {
        return PlayResponse().apply {
            isSuccessful = response.isSuccessful
            code = response.statusCode

            GooglePlayApi

            if (response.isSuccessful) {
                responseBytes = response.body().toByteArray()
            }

            if (response.isClientError || response.isServerError) {
                errorBytes = response.responseMessage.toByteArray()
                errorString = String(errorBytes)
            }
        }.also {
            Log.i("FUEL [${request.method}:${response.statusCode}] ${response.url}")
        }
    }
}