/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data

import android.util.Log
import com.aurora.Constants
import com.aurora.extensions.TAG
import com.aurora.store.data.model.ExodusTracker
import com.aurora.store.data.model.NewTrackersResponse
import com.aurora.store.data.model.ReportsResponse
import com.aurora.store.data.model.ReportsResult
import com.aurora.store.data.model.VersionEntry
import com.aurora.store.data.model.VersionsResponse
import java.io.IOException
import java.net.HttpURLConnection
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Headers.Companion.headersOf
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

@Singleton
class ExodusRepository @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json
) {

    private fun appUrl(packageName: String, endpoint: String): HttpUrl =
        Constants.AURORA_API_BASE_URL.toHttpUrl().newBuilder()
            .addPathSegment("exodus")
            .addPathSegment(packageName)
            .addPathSegment(endpoint)
            .build()

    /**
     * Returns the body of a successful response, or null when aurora-api has no reports for the
     * app (404). Throws for any other status.
     */
    private fun get(url: HttpUrl): String? {
        val request = Request(url = url, headers = headersOf("Accept", Constants.JSON_MIME_TYPE))
        return okHttpClient.newCall(request).execute().use { response ->
            when {
                response.isSuccessful -> response.body.string()
                response.code == HttpURLConnection.HTTP_NOT_FOUND -> null
                else -> throw IOException("Unexpected HTTP ${response.code} from aurora-api")
            }
        }
    }

    suspend fun fetchReports(packageName: String): ReportsResult = withContext(Dispatchers.IO) {
        try {
            val body = get(appUrl(packageName, "reports"))
                ?: return@withContext ReportsResult.NotFound
            val response = json.decodeFromString<ReportsResponse>(body)
            ReportsResult.Reports(
                reports = response.reports,
                trackers = response.trackers.entries
                    .mapNotNull { (id, tracker) ->
                        id.toIntOrNull()?.let { it to tracker.sanitized() }
                    }
                    .toMap()
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to fetch exodus reports", exception)
            ReportsResult.Failed
        }
    }

    suspend fun fetchVersions(packageName: String): List<VersionEntry> =
        withContext(Dispatchers.IO) {
            try {
                get(appUrl(packageName, "versions"))
                    ?.let { json.decodeFromString<VersionsResponse>(it).versions }
                    .orEmpty()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch exodus versions", exception)
                emptyList()
            }
        }

    /**
     * Trackers in the newest report that are not in the report of [installedVersionCode]; empty
     * when either report is missing, nothing is new, or the lookup fails.
     */
    suspend fun newTrackers(packageName: String, installedVersionCode: Long): List<ExodusTracker> =
        withContext(Dispatchers.IO) {
            try {
                val url = appUrl(packageName, "new-trackers").newBuilder()
                    .addQueryParameter("installed", installedVersionCode.toString())
                    .build()
                get(url)
                    ?.let { json.decodeFromString<NewTrackersResponse>(it).trackers }
                    .orEmpty()
                    .map { it.sanitized() }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch new exodus trackers", exception)
                emptyList()
            }
        }

    private fun ExodusTracker.sanitized(): ExodusTracker =
        if (url.toHttpUrlOrNull() != null) this else copy(url = String())
}
