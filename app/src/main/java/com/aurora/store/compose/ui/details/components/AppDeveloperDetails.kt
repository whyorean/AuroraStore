/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.aurora.extensions.browse
import com.aurora.extensions.copyToClipBoard
import com.aurora.extensions.mailTo
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.compose.composables.HeaderComposable
import com.aurora.store.compose.composables.InfoComposable
import com.aurora.store.compose.composables.preview.AppPreviewProvider

/**
 * Composable to display details of the app developer, supposed to be used as a part
 * of the Column with proper vertical arrangement spacing in the AppDetailsScreen.
 * @param address Address of the app developer
 * @param website Website of the app developer
 * @param email Email address of the app developer
 */
@Composable
fun AppDeveloperDetails(address: String, website: String, email: String) {
    val context = LocalContext.current

    HeaderComposable(title = stringResource(R.string.details_dev_details))
    Column {
        if (website.isNotBlank()) {
            InfoComposable(
                title = AnnotatedString(text = stringResource(R.string.details_dev_website)),
                description = AnnotatedString(text = website),
                icon = R.drawable.ic_network,
                onClick = { context.browse(website) }
            )
        }

        if (email.isNotBlank()) {
            InfoComposable(
                title = AnnotatedString(text = stringResource(R.string.details_dev_email)),
                description = AnnotatedString(text = email),
                icon = R.drawable.ic_mail,
                onClick = { context.mailTo(email) }
            )
        }

        if (address.isNotBlank()) {
            InfoComposable(
                title = AnnotatedString(text = stringResource(R.string.details_dev_address)),
                description = AnnotatedString.fromHtml(htmlString = address),
                icon = R.drawable.ic_person_location,
                onClick = { context.copyToClipBoard(address) }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppDeveloperDetailsPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_medium))) {
        AppDeveloperDetails(
            address = app.developerAddress,
            website = app.developerWebsite,
            email = app.developerEmail
        )
    }
}
