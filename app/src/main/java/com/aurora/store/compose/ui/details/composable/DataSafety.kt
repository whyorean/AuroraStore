/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.aurora.extensions.browse
import com.aurora.gplayapi.model.App
import com.aurora.gplayapi.model.DataSafetyEntry
import com.aurora.gplayapi.model.DataSafetyEntryType
import com.aurora.gplayapi.model.DataSafetyReport
import com.aurora.store.R
import com.aurora.store.compose.composable.Info
import com.aurora.store.compose.composable.SectionHeader
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.ThemePreviewProvider

/**
 * Composable to display app's data safety report, supposed to be used as a part
 * of the Column with proper vertical arrangement spacing in the AppDetailsScreen.
 * @param report App's data safety report
 * @param privacyPolicyUrl App's privacy policy URL
 */
@Composable
fun DataSafety(report: DataSafetyReport, privacyPolicyUrl: String) {
    val context = LocalContext.current

    SectionHeader(
        title = stringResource(R.string.details_data_safety_title),
        subtitle = stringResource(R.string.details_data_safety_subtitle),
        onClick = { context.browse(privacyPolicyUrl) }
    )

    report.entries.groupBy { it.type }.forEach { (type, entries) ->
        when (type) {
            DataSafetyEntryType.DATA_COLLECTED -> {
                Info(
                    painter = painterResource(R.drawable.ic_cloud_upload),
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

            DataSafetyEntryType.DATA_SHARED -> {
                Info(
                    painter = painterResource(R.drawable.ic_share),
                    title = AnnotatedString(
                        text = stringResource(R.string.details_data_safety_shared)
                    ),
                    description = AnnotatedString(
                        text = entries.first().subEntries.joinToString(", ") { it.name }
                            .ifBlank {
                                stringResource(R.string.details_data_safety_share_none)
                            }
                    )
                )
            }

            // We don't care about any other sections
            else -> {}
        }
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun DataSafetyPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
    ) {
        DataSafety(
            privacyPolicyUrl = app.privacyPolicyUrl,
            report = DataSafetyReport(
                packageName = app.packageName,
                entries = listOf(
                    DataSafetyEntry(
                        type = DataSafetyEntryType.DATA_COLLECTED,
                        name = String(),
                        description = String()
                    ),
                    DataSafetyEntry(
                        type = DataSafetyEntryType.DATA_SHARED,
                        name = String(),
                        description = String()
                    )
                )
            )
        )
    }
}
