/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil3.request.ImageRequest
import coil3.request.crossfade

/** Reuses equivalent Coil requests while list rows recompose for unrelated UI state changes. */
@Composable
internal fun rememberStoreImageRequest(data: Any?): ImageRequest {
    val context = LocalContext.current
    return remember(context, data) {
        ImageRequest.Builder(context)
            .data(data)
            .crossfade(true)
            .build()
    }
}
