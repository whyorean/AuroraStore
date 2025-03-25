/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.LocalAsyncImagePreviewHandler
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.compose.composables.HeaderComposable
import com.aurora.store.compose.composables.InfoComposable
import com.aurora.store.compose.composables.TopAppBarComposable
import com.aurora.store.compose.composables.preview.AppPreviewProvider
import com.aurora.store.compose.composables.preview.coilPreviewProvider
import com.aurora.store.viewmodel.details.AppDetailsViewModel
import java.util.Locale

@Composable
fun DetailsMoreScreen(
    onNavigateUp: () -> Unit,
    viewModel: AppDetailsViewModel = hiltViewModel()
) {
    val app by viewModel.app.collectAsStateWithLifecycle()

    ScreenContent(app = app!!, onNavigateUp = onNavigateUp)
}

@Composable
private fun ScreenContent(app: App, onNavigateUp: () -> Unit = {}) {
    Scaffold(
        topBar = { TopAppBarComposable(title = app.displayName, onNavigateUp = onNavigateUp) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_medium))
        ) {
            HeaderComposable(title = stringResource(R.string.details_description))
            Text(
                modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.padding_medium)),
                text = AnnotatedString.fromHtml(
                    htmlString = app.description
                ),
                style = MaterialTheme.typography.bodyMedium
            )

            if (app.appInfo.appInfoMap.isNotEmpty()) {
                HeaderComposable(title = stringResource(R.string.details_more_info))

                InfoComposable(
                    title = AnnotatedString(
                        text = stringResource(R.string.details_more_package_name)
                    ),
                    description = AnnotatedString(text = app.packageName)
                )

                InfoComposable(
                    title = AnnotatedString(
                        text = stringResource(R.string.details_more_content_rating)
                    ),
                    description = AnnotatedString(text = app.contentRating.title)
                )

                app.appInfo.appInfoMap.forEach { (title, subtitle) ->
                    InfoComposable(
                        title = AnnotatedString(
                            text = title.replace("_", " ")
                                .lowercase(Locale.getDefault())
                                .replaceFirstChar {
                                    if (it.isLowerCase()) {
                                        it.titlecase(Locale.getDefault())
                                    } else {
                                        it.toString()
                                    }
                                }
                        ),
                        description = AnnotatedString(text = subtitle)
                    )
                }

                InfoComposable(
                    title = AnnotatedString(
                        text = stringResource(R.string.details_more_target_api)
                    ),
                    description = AnnotatedString(text = "API ${app.targetSdk}")
                )
            }
        }
    }
}

@Preview
@Composable
@OptIn(ExperimentalCoilApi::class)
private fun DetailsMoreScreenPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    CompositionLocalProvider(LocalAsyncImagePreviewHandler provides coilPreviewProvider) {
        ScreenContent(app = app)
    }
}
