/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Filter for list with search results
 */
@Parcelize
@Serializable
data class SearchFilter(
    val noAds: Boolean = false,
    val isFree: Boolean = false,
    val noGMS: Boolean = false,
    val minRating: Float = 0F,
    val minInstalls: Long = 0
) : Parcelable
