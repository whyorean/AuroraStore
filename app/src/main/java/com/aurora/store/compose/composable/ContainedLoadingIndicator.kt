/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import com.aurora.store.R
import com.aurora.store.compose.preview.PreviewTemplate

/**
 * Composable to display an indeterminate loading indicator that fills all available screen
 * @param modifier The modifier to be applied to the composable
 */
@Composable
fun ContainedLoadingIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.padding_small)),
        contentAlignment = Alignment.Center
    ) {
        val description = stringResource(R.string.loading)
        ContainedLoadingIndicator(
            modifier = Modifier
                .requiredSize(dimensionResource(R.dimen.icon_size_small))
                .semantics { stateDescription = description }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ContainedLoadingIndicatorPreview() {
    PreviewTemplate {
        ContainedLoadingIndicator()
    }
}
