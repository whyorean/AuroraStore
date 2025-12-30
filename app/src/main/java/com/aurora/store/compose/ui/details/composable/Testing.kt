/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.compose.composable.Header
import com.aurora.store.compose.composable.Info
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.PreviewTemplate

/**
 * Composable to display app's beta testing status, supposed to be used as a part of the
 * Column with proper vertical arrangement spacing in the AppDetailsScreen.
 *
 * @param isSubscribed Whether the user is subscribed to the beta testing program
 * @param onTestingSubscriptionChange Callback when the the subscription button is clicked
 */
@Composable
fun Testing(isSubscribed: Boolean, onTestingSubscriptionChange: (subscribe: Boolean) -> Unit = {}) {
    Header(title = stringResource(R.string.details_beta))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_small))
    ) {
        Info(
            modifier = Modifier.weight(1F),
            painter = painterResource(R.drawable.ic_experiment),
            title = AnnotatedString(
                text = if (isSubscribed) {
                    stringResource(R.string.details_beta_subscribed)
                } else {
                    stringResource(R.string.details_beta_available)
                }
            ),
            description = AnnotatedString(text = stringResource(R.string.details_beta_description))
        )
        FilledTonalButton(onClick = { onTestingSubscriptionChange(!isSubscribed) }) {
            Text(
                text = if (isSubscribed) {
                    stringResource(R.string.action_leave)
                } else {
                    stringResource(R.string.action_join)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TestingPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    PreviewTemplate {
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_medium))
        ) {
            Testing(isSubscribed = app.testingProgram!!.isSubscribed)
        }
    }
}
