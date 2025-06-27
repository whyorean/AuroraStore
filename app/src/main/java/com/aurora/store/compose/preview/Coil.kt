/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.preview

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import coil3.ColorImage
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImagePreviewHandler

/**
 * Preview provider for composable working with coil for loading remote images
 */
@OptIn(ExperimentalCoilApi::class)
val coilPreviewProvider = AsyncImagePreviewHandler {
    ColorImage(Color.Gray.toArgb())
}
