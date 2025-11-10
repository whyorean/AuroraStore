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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurora.extensions.adaptiveNavigationIcon
import com.aurora.extensions.isWindowCompact
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.compose.composable.Header
import com.aurora.store.compose.composable.Info
import com.aurora.store.compose.composable.TopAppBar
import com.aurora.store.compose.composable.app.AppListItem
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.PreviewTemplate
import com.aurora.store.viewmodel.details.AppDetailsViewModel
import com.aurora.store.viewmodel.details.MoreViewModel
import java.util.Locale

@Composable
fun MoreScreen(
    packageName: String,
    onNavigateUp: () -> Unit,
    onNavigateToAppDetails: (packageName: String) -> Unit,
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
    val topAppBarTitle = when {
        windowAdaptiveInfo.isWindowCompact -> app.displayName
        else -> stringResource(R.string.details_more_about_app)
    }

    Scaffold(
        topBar = {
            TopAppBar(
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
            Header(title = stringResource(R.string.details_description))
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
private fun MoreScreenPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    PreviewTemplate {
        ScreenContent(app = app)
    }
}
