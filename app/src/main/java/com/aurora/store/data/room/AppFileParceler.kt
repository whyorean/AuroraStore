/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.room

import android.os.Parcel
import com.aurora.gplayapi.model.AppFile
import kotlinx.parcelize.Parceler
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Round-trips an [AppFile] through the download/library/update Room entities' `Parcelable`
 * conformance as JSON.
 *
 * gplayapi-v4 models are deliberately never `Parcelable` (`core` has no Android dependency
 * at all), so [com.aurora.store.data.room.download.Download],
 * [com.aurora.store.data.room.download.SharedLib],
 * [com.aurora.store.data.room.suite.ExternalApk] and [com.aurora.store.data.room.update.Update]
 * need this to keep parcelling their `fileList`, the same way
 * [com.aurora.store.compose.navigation.StreamClusterParceler] bridges [AppFile]'s sibling
 * models across `Screen`'s backstack.
 */
internal object AppFileParceler : Parceler<AppFile> {
    override fun create(parcel: Parcel): AppFile =
        Json.decodeFromString(parcel.readString().orEmpty())

    override fun AppFile.write(parcel: Parcel, flags: Int) {
        parcel.writeString(Json.encodeToString(this))
    }
}
