/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.model

import android.content.Context
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.Artwork
import com.aurora.gplayapi.data.models.EncodedCertificateSet
import com.aurora.gplayapi.data.models.PlayFile
import com.aurora.store.R
import com.aurora.store.util.CertUtil
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Self-update feed entry returned by `release_feed.json` (vanilla release) and
 * `nightly_feed.json` (nightly), both served from the Aurora OSS server.
 *
 * The producer encodes numeric fields as JSON strings, so the raw fields below stay
 * `String` and we expose typed `Long` accessors that tolerate blank values. Decoding
 * with `Long`s would otherwise blow up on the first request.
 */
@Serializable
data class SelfUpdate(
    @SerialName("version_name")
    val versionName: String = "",
    @SerialName("version_code")
    val versionCodeRaw: String = "0",
    @SerialName("download_url")
    val downloadUrl: String = "",
    @SerialName("icon_url")
    val iconUrl: String = "",
    @SerialName("sha1")
    val sha1: String = "",
    @SerialName("sha256")
    val sha256: String = "",
    val changelog: String = "",
    @SerialName("size")
    val sizeRaw: String = "0",
    @SerialName("last_commit")
    val lastCommit: String = "",
    @SerialName("updated_on")
    val updatedOn: String = "",
    @SerialName("timestamp")
    val timestampRaw: String = "0"
) {
    val versionCode: Long get() = versionCodeRaw.toLongOrNull() ?: 0L
    val size: Long get() = sizeRaw.toLongOrNull() ?: 0L
    val timestamp: Long get() = timestampRaw.toLongOrNull() ?: 0L

    /**
     * Maps the feed entry onto a regular [App] so it can flow through the normal
     * update pipeline ([com.aurora.store.data.room.update.Update.fromApp] →
     * download → install). The certificate set is the currently installed app's own
     * hashes so [com.aurora.store.data.room.update.Update.hasValidCert] holds and the
     * update is never filtered out as untrusted.
     */
    fun toApp(context: Context): App = App(
        packageName = context.packageName,
        versionCode = versionCode,
        versionName = versionName,
        changes = changelog,
        size = size,
        updatedOn = updatedOn,
        displayName = context.getString(R.string.app_name),
        developerName = "Rahul Kumar Patel",
        iconArtwork = Artwork(url = iconUrl),
        fileList = mutableListOf(
            PlayFile(
                name = "${context.packageName}.apk",
                url = downloadUrl,
                size = size,
                sha1 = sha1,
                sha256 = sha256
            )
        ),
        isFree = true,
        isInstalled = true,
        certificateSetList = CertUtil.getEncodedCertificateHashes(
            context,
            context.packageName
        ).map {
            EncodedCertificateSet(certificateSet = it, sha256 = String())
        }.toMutableList()
    )
}
