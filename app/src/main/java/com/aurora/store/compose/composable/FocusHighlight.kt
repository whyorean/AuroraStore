/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aurora.store.R
import com.aurora.store.compose.composition.LocalUI
import com.aurora.store.compose.composition.UI

/**
 * Draws a ring around this node while it holds D-pad focus. A remote has no cursor, no hover and
 * no finger to ripple under, so without this a focused row is indistinguishable from the rest.
 * TV only, everywhere else this is a no-op. Put it next to the `clickable` (or any other focusable)
 * that created the focus target it should decorate.
 */
@Composable
fun Modifier.focusHighlight(width: Dp = 2.dp): Modifier {
    if (LocalUI.current != UI.TV) {
        return this
    }

    val color = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(dimensionResource(R.dimen.radius_small))
    var isFocused by remember { mutableStateOf(false) }
    return this
        .onFocusChanged { isFocused = it.isFocused }
        // border(0.dp) is not a no-op: the node still draws, Skia just strokes it 1px wide.
        .then(if (isFocused) Modifier.border(width, color, shape) else Modifier)
}
