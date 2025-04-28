/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
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
import androidx.window.core.layout.WindowWidthSizeClass
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.LocalAsyncImagePreviewHandler
import com.aurora.extensions.adaptiveNavigationIcon
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.compose.composables.HeaderComposable
import com.aurora.store.compose.composables.InfoComposable
import com.aurora.store.compose.composables.TopAppBarComposable
import com.aurora.store.compose.composables.app.AppComposable
import com.aurora.store.compose.composables.preview.AppPreviewProvider
import com.aurora.store.compose.composables.preview.coilPreviewProvider
import com.aurora.store.viewmodel.details.AppDetailsViewModel
import com.aurora.store.viewmodel.details.DetailsMoreViewModel
import java.util.Locale

@Composable
fun DetailsMoreScreen(
    onNavigateUp: () -> Unit,
    onNavigateToAppDetails: (packageName: String) -> Unit,
    appDetailsViewModel: AppDetailsViewModel = hiltViewModel(),
    detailsMoreViewModel: DetailsMoreViewModel = hiltViewModel { factory: DetailsMoreViewModel.Factory ->
        factory.create(appDetailsViewModel.app.value!!.dependencies.dependentPackages)
    }
) {
    val app by appDetailsViewModel.app.collectAsStateWithLifecycle()
    val dependencies by detailsMoreViewModel.dependentApps.collectAsStateWithLifecycle()

    ScreenContent(
        app = app!!,
        dependencies = dependencies,
        onNavigateUp = onNavigateUp,
        onNavigateToAppDetails = onNavigateToAppDetails
    )
}

@Composable
private fun ScreenContent(
    app: App,
    dependencies: List<App>? = null,
    onNavigateUp: () -> Unit = {},
    onNavigateToAppDetails: (packageName: String) -> Unit = {},
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo()
) {
    val topAppBarTitle = when (windowAdaptiveInfo.windowSizeClass.windowWidthSizeClass) {
        WindowWidthSizeClass.COMPACT -> app.displayName
        else -> stringResource(R.string.details_more_about_app)
    }

    Scaffold(
        topBar = {
            TopAppBarComposable(
                title = topAppBarTitle,
                navigationIcon = windowAdaptiveInfo.adaptiveNavigationIcon,
                onNavigateUp = onNavigateUp
            )
        }
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

            if (dependencies != null) {
                AppDependencies(
                    dependencies = dependencies,
                    onNavigateToAppDetails = onNavigateToAppDetails
                )
            }

            AppInfoMore(app = app)
        }
    }
}

/**
 * Composable to show dependencies of an app
 */
@Composable
private fun AppDependencies(
    dependencies: List<App>,
    onNavigateToAppDetails: (packageName: String) -> Unit
) {
    HeaderComposable(title = stringResource(R.string.details_dependencies))
    if (dependencies.isEmpty()) {
        InfoComposable(
            title = AnnotatedString(text = stringResource(R.string.details_no_dependencies))
        )
    } else {
        LazyRow(modifier = Modifier.fillMaxWidth()) {
            items(items = dependencies, key = { item -> item.id }) { app ->
                AppComposable(
                    app = app,
                    onClick = { onNavigateToAppDetails(app.packageName) }
                )
            }
        }
    }
}

/**
 * Composable to show more information about the app that maybe advanced
 */
@Composable
private fun AppInfoMore(app: App) {
    HeaderComposable(title = stringResource(R.string.details_more_info))
    InfoComposable(
        title = AnnotatedString(
            text = stringResource(R.string.details_more_package_name)
        ),
        description = AnnotatedString(text = app.packageName)
    )

    InfoComposable(
        title = AnnotatedString(
            text = stringResource(R.string.details_more_target_api)
        ),
        description = AnnotatedString(text = "API ${app.targetSdk}")
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
}

@Preview
@Composable
@OptIn(ExperimentalCoilApi::class)
private fun DetailsMoreScreenPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    CompositionLocalProvider(LocalAsyncImagePreviewHandler provides coilPreviewProvider) {
        ScreenContent(app = app)
    }
}
