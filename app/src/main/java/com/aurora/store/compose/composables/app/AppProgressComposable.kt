/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composables.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.aurora.store.R

/**
 * Composable to display an indeterminate circular progress indicator
 * @param modifier The modifier to be applied to the composable
 */
@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun AppProgressComposable(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.padding_small)),
        contentAlignment = Alignment.Center
    ) {
        ContainedLoadingIndicator(
            modifier = Modifier.requiredSize(dimensionResource(R.dimen.icon_size_small))
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppProgressComposablePreview() {
    AppProgressComposable()
}
