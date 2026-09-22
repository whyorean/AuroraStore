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
import kotlinx.serialization.SerialName
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
data class ExodusReport(
    val creator: String = String(),
    val name: String = String(),
    val reports: List<Report> = listOf()
)

@Serializable
@Parcelize
data class Report(
    val id: Int = -1,
    val downloads: String = String(),
    val version: String = String(),
    @SerialName("creation_date") val creationDate: String = String(),
    @SerialName("updated_at") val updatedAt: String = String(),
    @SerialName("version_code") val versionCode: String = String(),
    val trackers: List<Int> = listOf()
) : Parcelable

@Serializable
data class ExodusTracker(
    val id: Int = 0,
    val name: String = String(),
    val url: String = String(),
    val signature: String = String(),
    val date: String = String(),
    val description: String = String(),
    val networkSignature: String = String(),
    val documentation: List<String> = emptyList(),
    val categories: List<String> = emptyList()
) {

    override fun hashCode(): Int = id

    override fun equals(other: Any?): Boolean = when (other) {
        is ExodusTracker -> other.id == id
        else -> false
    }
}

@Serializable
data class TrackersResponse(
    val trackers: Map<String, TrackerDto> = emptyMap()
)

@Serializable
data class TrackerDto(
    val id: Int = 0,
    val name: String = String(),
    @SerialName("website") val website: String = String(),
    @SerialName("code_signature") val codeSignature: String = String(),
    @SerialName("creation_date") val creationDate: String = String(),
    val categories: List<String> = emptyList()
)
