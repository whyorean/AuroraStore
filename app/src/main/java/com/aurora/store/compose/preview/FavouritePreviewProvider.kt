/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.aurora.store.BuildConfig
import com.aurora.store.data.room.favourite.Favourite
import com.aurora.store.data.room.favourite.Favourite.Mode

/**
 * Preview provider for composable working with [Favourite]
 */
class FavouritePreviewProvider() : PreviewParameterProvider<Favourite> {

    override val values: Sequence<Favourite>
        get() = sequenceOf(
            Favourite(
                packageName = BuildConfig.APPLICATION_ID,
                displayName = "Aurora Store",
                iconURL = "",
                added = 0L,
                mode = Mode.MANUAL
            )
        )
}
