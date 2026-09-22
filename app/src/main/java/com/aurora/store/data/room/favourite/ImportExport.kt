/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.room.favourite

import com.aurora.store.BuildConfig
import kotlinx.serialization.Serializable

@Serializable
data class ImportExport(
    val favourites: List<Favourite>,
    val auroraStoreVersion: Int = BuildConfig.VERSION_CODE
)
