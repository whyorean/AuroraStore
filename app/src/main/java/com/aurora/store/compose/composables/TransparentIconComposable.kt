/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composables

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.aurora.store.R

/**
 * A transparent icon for occupying space
 *
 * This is useful for occupying spaces in composable where alignment is not respected such as
 * DropDownMenu.
 * @param modifier The modifier to be applied to the composable
 */
@Composable
fun TransparentIconComposable(modifier: Modifier = Modifier) {
    Icon(
        modifier = modifier,
        painter = painterResource(R.drawable.ic_transparent),
        contentDescription = null
    )
}

@Preview(showBackground = true)
@Composable
private fun TransparentIconComposablePreview() {
    TransparentIconComposable()
}
