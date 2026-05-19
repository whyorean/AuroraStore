/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurora.extensions.adaptiveNavigationIcon
import com.aurora.extensions.isWindowCompact
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.compose.composable.Header
import com.aurora.store.compose.composable.Info
import com.aurora.store.compose.composable.ScrollHint
import com.aurora.store.compose.composable.TopAppBar
import com.aurora.store.compose.composable.app.AppListItem
import com.aurora.store.compose.navigation.Destination
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.ThemePreviewProvider
import com.aurora.store.viewmodel.details.AppDetailsViewModel
import com.aurora.store.viewmodel.details.MoreViewModel

@Composable
fun MoreScreen(
    packageName: String,
    onNavigateTo: (Destination) -> Unit,
    appDetailsViewModel: AppDetailsViewModel = hiltViewModel(key = packageName),
    moreViewModel: MoreViewModel = hiltViewModel(
        key = "$packageName/more",
        creationCallback = { factory: MoreViewModel.Factory ->
            factory.create(appDetailsViewModel.app.value!!.dependencies.dependentPackages)
        }
    )
) {
    val app by appDetailsViewModel.app.collectAsStateWithLifecycle()
    val dependencies by moreViewModel.dependentApps.collectAsStateWithLifecycle()

    ScreenContent(
        app = app!!,
        dependencies = dependencies,
        onNavigateTo = onNavigateTo
    )
}

@Composable
private fun ScreenContent(
    app: App,
    dependencies: List<App>? = null,
    onNavigateTo: (Destination) -> Unit = {},
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfoV2()
) {
    val topAppBarTitle = when {
        windowAdaptiveInfo.isWindowCompact -> app.displayName
        else -> stringResource(R.string.details_more_about_app)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = topAppBarTitle,
                navigationIcon = windowAdaptiveInfo.adaptiveNavigationIcon
            )
        }
    ) { paddingValues ->
        val listState = rememberLazyListState()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_medium))
            ) {
                item {
                    Header(title = stringResource(R.string.details_description))
                }

                item {
                    Text(
                        modifier = Modifier.padding(
                            horizontal = dimensionResource(R.dimen.padding_medium)
                        ),
                        text = AnnotatedString.fromHtml(
                            htmlString = app.description
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                item {
                    if (dependencies != null) {
                        AppDependencies(
                            dependencies = dependencies,
                            onNavigateTo = onNavigateTo
                        )
                    }
                }

                item {
                    AppInfoMore(app = app)
                }
            }
            ScrollHint(
                listState = listState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/**
 * Composable to show dependencies of an app
 */
@Composable
private fun AppDependencies(dependencies: List<App>, onNavigateTo: (Destination) -> Unit) {
    Header(title = stringResource(R.string.details_dependencies))
    if (dependencies.isEmpty()) {
        Info(
            title = AnnotatedString(text = stringResource(R.string.details_no_dependencies))
        )
    } else {
        LazyRow(modifier = Modifier.fillMaxWidth()) {
            items(items = dependencies, key = { item -> item.id }) { app ->
                AppListItem(
                    app = app,
                    onClick = { onNavigateTo(Destination.AppDetails(app.packageName)) }
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
    Header(title = stringResource(R.string.details_more_info))
    Info(
        title = AnnotatedString(
            text = stringResource(R.string.details_more_package_name)
        ),
        description = AnnotatedString(text = app.packageName)
    )

    Info(
        title = AnnotatedString(
            text = stringResource(R.string.details_more_target_api)
        ),
        description = AnnotatedString(text = "API ${app.targetSdk}")
    )

    Info(
        title = AnnotatedString(
            text = stringResource(R.string.details_more_content_rating)
        ),
        description = AnnotatedString(text = app.contentRating.title)
    )

    app.appInfo.appInfoMap.forEach { (title, subtitle) ->
        Info(
            title = AnnotatedString(
                text = title.replace("_", " ")
                    .lowercase(LocalLocale.current.platformLocale)
                    .replaceFirstChar {
                        if (it.isLowerCase()) {
                            it.titlecase(LocalLocale.current.platformLocale)
                        } else {
                            it.toString()
                        }
                    }
            ),
            description = AnnotatedString(text = subtitle)
        )
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview
@Composable
private fun MoreScreenPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    ScreenContent(app = app)
}
