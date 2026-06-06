/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.commons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aurora.store.R
import com.aurora.store.compose.preview.ThemePreviewProvider

/**
 * A non-dismissable dialog showing a spinner alongside a short status message, for blocking
 * operations the user must wait on (e.g. switching account, adding account).
 * @param message Status message to display next to the spinner
 */
@Composable
fun LoadingDialog(message: String) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(dimensionResource(R.dimen.radius_large)),
            tonalElevation = dimensionResource(R.dimen.spacing_xsmall)
        ) {
            Row(
                modifier = Modifier.padding(dimensionResource(R.dimen.spacing_large)),
                horizontalArrangement = Arrangement.spacedBy(
                    dimensionResource(R.dimen.spacing_large)
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator()
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview
@Composable
private fun LoadingDialogPreview() {
    LoadingDialog(message = stringResource(R.string.account_switching))
}
