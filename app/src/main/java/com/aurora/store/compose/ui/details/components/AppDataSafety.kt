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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.aurora.extensions.browse
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.datasafety.Entry
import com.aurora.gplayapi.data.models.datasafety.EntryType
import com.aurora.gplayapi.data.models.datasafety.Report
import com.aurora.store.R
import com.aurora.store.compose.composables.HeaderComposable
import com.aurora.store.compose.composables.InfoComposable
import com.aurora.store.compose.composables.preview.AppPreviewProvider

/**
 * Composable to display app's data safety report, supposed to be used as a part
 * of the Column with proper vertical arrangement spacing in the AppDetailsScreen.
 * @param report App's data safety report
 * @param privacyPolicyUrl App's privacy policy URL
 */
@Composable
fun AppDataSafety(report: Report, privacyPolicyUrl: String) {
    val context = LocalContext.current

    HeaderComposable(
        title = stringResource(R.string.details_data_safety_title),
        subtitle = stringResource(R.string.details_data_safety_subtitle),
        onClick = { context.browse(privacyPolicyUrl) }
    )

    report.entries.groupBy { it.type }.forEach { (type, entries) ->
        when (type) {
            EntryType.DATA_COLLECTED -> {
                InfoComposable(
                    icon = R.drawable.ic_cloud_upload,
                    title = AnnotatedString(
                        text = stringResource(R.string.details_data_safety_collect)
                    ),
                    description = AnnotatedString(
                        text = entries.first().subEntries.joinToString(", ") { it.name }
                            .ifBlank {
                                stringResource(R.string.details_data_safety_collect_none)
                            }
                    )
                )
            }

            EntryType.DATA_SHARED -> {
                InfoComposable(
                    icon = R.drawable.ic_share,
                    title = AnnotatedString(
                        text = stringResource(R.string.details_data_safety_shared)
                    ),
                    description = AnnotatedString(
                        text = entries.first().subEntries.joinToString(", ") { it.name }
                            .ifBlank {
                                stringResource(R.string.details_data_safety_share_none)
                            }
                    ),
                )
            }

            // We don't care about any other sections
            else -> {}
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppDataSafetyPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_medium))) {
        AppDataSafety(
            privacyPolicyUrl = app.privacyPolicyUrl,
            report = Report(
                packageName = app.packageName,
                entries = listOf(
                    Entry(type = EntryType.DATA_COLLECTED, name = String(), description = String()),
                    Entry(type = EntryType.DATA_SHARED, name = String(), description = String())
                )
            )
        )
    }
}
