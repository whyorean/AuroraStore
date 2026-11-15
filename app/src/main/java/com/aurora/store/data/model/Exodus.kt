/*
 * SPDX-FileCopyrightText: 2021 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.model

import android.os.Parcelable
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Formats an Exodus date string (e.g. `2023-01-01T12:34:56Z`) into a localized medium date.
 * Falls back to the raw value if it cannot be parsed.
 */
fun formatExodusDate(date: String): String {
    val datePart = date.take(10)
    return try {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(datePart)
        if (parsed != null) DateFormat.getDateInstance(DateFormat.MEDIUM).format(parsed) else date
    } catch (_: Exception) {
        date
    }
}

@Serializable
@Parcelize
data class Report(
    val id: Int = -1,
    val version: String = String(),
    val versionCode: Long = 0L,
    val date: String = String(),
    val trackers: List<Int> = listOf()
) : Parcelable

@Serializable
data class ExodusTracker(
    val name: String = String(),
    val url: String = String(),
    val signature: String = String(),
    val categories: List<String> = emptyList()
)

@Serializable
data class VersionEntry(
    val versionCode: Long = 0L,
    val version: String = String(),
    val date: String = String()
)

sealed interface ReportsResult {
    data class Reports(
        val reports: List<Report>,
        val trackers: Map<Int, ExodusTracker>
    ) : ReportsResult

    data object NotFound : ReportsResult

    data object Failed : ReportsResult
}

/**
 * Resolves the tracker ids of this report against [catalogue], best-effort: ids missing from it
 * resolve to a `Tracker #<id>` placeholder.
 */
fun Report.resolveTrackers(catalogue: Map<Int, ExodusTracker>): List<ExodusTracker> =
    trackers.map { id -> catalogue[id] ?: ExodusTracker(name = "Tracker #$id") }

@Serializable
data class ReportsResponse(
    val reports: List<Report> = emptyList(),
    val trackers: Map<String, ExodusTracker> = emptyMap()
)

@Serializable
data class VersionsResponse(val versions: List<VersionEntry> = emptyList())

@Serializable
data class NewTrackersResponse(val trackers: List<ExodusTracker> = emptyList())
