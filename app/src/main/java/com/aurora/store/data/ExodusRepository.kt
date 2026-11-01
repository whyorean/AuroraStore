/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data

import android.content.Context
import android.util.Log
import com.aurora.Constants
import com.aurora.extensions.TAG
import com.aurora.store.BuildConfig
import com.aurora.store.data.model.ExodusReport
import com.aurora.store.data.model.ExodusTracker
import com.aurora.store.data.model.Report
import com.aurora.store.data.model.TrackersResponse
import com.aurora.store.data.room.exodus.TrackerDao
import com.aurora.store.data.room.exodus.TrackerEntity
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_LAST_TRACKER_SYNC
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Headers.Companion.toHeaders
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

@Singleton
class ExodusRepository @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val trackerDao: TrackerDao,
    @ApplicationContext private val context: Context
) {

    companion object {
        private val SYNC_INTERVAL_MS = TimeUnit.DAYS.toMillis(3)
        private const val CATEGORY_SEPARATOR = "\n"
    }

    private val authHeaders: Map<String, String>
        get() = mapOf(
            "Content-Type" to Constants.JSON_MIME_TYPE,
            "Accept" to Constants.JSON_MIME_TYPE,
            "Authorization" to "Token ${BuildConfig.EXODUS_API_KEY}"
        )

    private fun get(url: String, headers: Map<String, String>): String {
        val request = Request(url = url.toHttpUrl(), headers = headers.toHeaders())
        return okHttpClient.newCall(request).execute().use { response ->
            response.body.string()
        }
    }

    suspend fun fetchReports(packageName: String): List<Report> = withContext(Dispatchers.IO) {
        try {
            val response = get("${Constants.EXODUS_SEARCH_URL}$packageName", authHeaders)
            parseReports(response, packageName)
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to fetch exodus reports", exception)
            emptyList()
        }
    }

    private fun parseReports(response: String, packageName: String): List<Report> = try {
        val exodusObject = JSONObject(response).getJSONObject(packageName)
        json.decodeFromString<ExodusReport>(exodusObject.toString()).reports
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun syncTrackersIfStale() = withContext(Dispatchers.IO) {
        val lastSync = Preferences.getLong(context, PREFERENCE_LAST_TRACKER_SYNC)
        val isStale = System.currentTimeMillis() - lastSync > SYNC_INTERVAL_MS
        if (trackerDao.count() > 0 && !isStale) return@withContext

        val response = get(Constants.EXODUS_TRACKERS_URL, authHeaders)
        val trackers = json.decodeFromString<TrackersResponse>(response)
            .trackers.values.map {
                TrackerEntity(
                    id = it.id,
                    name = it.name,
                    url = it.website,
                    signature = it.codeSignature,
                    date = it.creationDate,
                    categories = it.categories.joinToString(CATEGORY_SEPARATOR)
                )
            }

        if (trackers.isNotEmpty()) {
            trackerDao.purgeAndInsert(trackers)
            Preferences.putLong(context, PREFERENCE_LAST_TRACKER_SYNC, System.currentTimeMillis())
        }
    }

    suspend fun resolveTrackers(ids: List<Int>): List<ExodusTracker> {
        if (ids.isEmpty()) return emptyList()
        val byId = trackerDao.getByIds(ids).associateBy { it.id }
        return ids.map { id ->
            byId[id]?.let {
                ExodusTracker(
                    id = it.id,
                    name = it.name,
                    url = it.url,
                    signature = it.signature,
                    date = it.date,
                    categories = if (it.categories.isBlank()) {
                        emptyList()
                    } else {
                        it.categories.split(CATEGORY_SEPARATOR)
                    }
                )
            } ?: ExodusTracker(id = id, name = "Tracker #$id")
        }
    }

    suspend fun getNewTrackers(
        packageName: String,
        installedVersionCode: Long
    ): List<ExodusTracker> = computeNewTrackers(fetchReports(packageName), installedVersionCode)

    suspend fun computeNewTrackers(
        reports: List<Report>,
        installedVersionCode: Long
    ): List<ExodusTracker> {
        if (reports.isEmpty()) return emptyList()

        val installedReport = reports.firstOrNull {
            it.versionCode.toLongOrNull() == installedVersionCode
        } ?: return emptyList()

        val newestReport = reports.maxByOrNull { it.versionCode.toLongOrNull() ?: -1L }
            ?: return emptyList()

        val newestVersionCode = newestReport.versionCode.toLongOrNull() ?: return emptyList()
        if (newestVersionCode <= installedVersionCode) return emptyList()

        val newIds = newestReport.trackers.toSet() - installedReport.trackers.toSet()
        return resolveTrackers(newIds.toList())
    }
}
