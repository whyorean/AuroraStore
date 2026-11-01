/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.navigation

import android.os.Parcel
import com.aurora.gplayapi.model.StreamCluster
import kotlinx.parcelize.Parceler
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Round-trips a [StreamCluster] through [Screen]'s `Parcelable` backstack as JSON.
 *
 * gplayapi-v4 models are deliberately never `Parcelable` (`core` has no Android dependency
 * at all), but [Screen.StreamBrowse] still needs to carry a whole, already-fetched
 * [StreamCluster] - not just an id - so its apps show immediately without a second network
 * round trip, the same way v3's `Parcelable` [StreamCluster] did. Every [StreamCluster] field
 * is itself `@Serializable`, so JSON is a generic bridge here rather than a field-by-field
 * `Parceler` that would have to be kept in sync with the model by hand.
 */
internal object StreamClusterParceler : Parceler<StreamCluster> {
    override fun create(parcel: Parcel): StreamCluster =
        Json.decodeFromString(parcel.readString().orEmpty())

    override fun StreamCluster.write(parcel: Parcel, flags: Int) {
        parcel.writeString(Json.encodeToString(this))
    }
}
