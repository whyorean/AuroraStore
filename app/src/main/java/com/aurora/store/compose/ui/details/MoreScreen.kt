/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
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
import com.aurora.gplayapi.data.models.PlayFile
import com.aurora.store.R
import com.aurora.store.compose.composable.Info
import com.aurora.store.compose.composable.ScrollHint
import com.aurora.store.compose.composable.SectionHeader
import com.aurora.store.compose.composable.TopAppBar
import com.aurora.store.compose.composable.app.AppListItem
import com.aurora.store.compose.ui.details.composable.DetailsPortalCard
import com.aurora.store.compose.navigation.Destination
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.ThemePreviewProvider
import com.aurora.store.viewmodel.details.AppDetailsViewModel
import com.aurora.store.viewmodel.details.MoreViewModel

@Composable
fun MoreScreen(
    packageName: String,
    onNavigateTo: (Destination) -> Unit,
    onOpenMoreInfo: () -> Unit,
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
        onNavigateTo = onNavigateTo,
        onOpenMoreInfo = onOpenMoreInfo
    )
}

@Composable
private fun ScreenContent(
    app: App,
    dependencies: List<App>? = null,
    onNavigateTo: (Destination) -> Unit = {},
    onOpenMoreInfo: () -> Unit = {},
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
                navigationIcon = windowAdaptiveInfo.adaptiveNavigationIcon,
                boxedNavigationIcon = true
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
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
                contentPadding = PaddingValues(
                    bottom = WindowInsets.navigationBars.asPaddingValues()
                        .calculateBottomPadding() + dimensionResource(R.dimen.spacing_large)
                ),
                verticalArrangement = Arrangement.spacedBy(
                    dimensionResource(R.dimen.spacing_large)
                )
            ) {
                item {
                    SectionHeader(title = stringResource(R.string.details_description))
                }

                item {
                    Text(
                        modifier = Modifier.padding(
                            horizontal = dimensionResource(R.dimen.spacing_large)
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

                if (app.fileList.isNotEmpty()) {
                    item {
                        AppFiles(files = app.fileList)
                    }
                }

                item {
                    DetailsPortalCard(
                        title = stringResource(R.string.details_more_info),
                        description = stringResource(R.string.details_more_info_description),
                        icon = painterResource(R.drawable.ic_menu_about),
                        onClick = onOpenMoreInfo
                    )
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
    SectionHeader(title = stringResource(R.string.details_dependencies))
    if (dependencies.isEmpty()) {
        Info(
            title = AnnotatedString(text = stringResource(R.string.details_no_dependencies))
        )
    } else {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.spacing_large)),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
        ) {
            items(items = dependencies, key = { item -> item.id }) { app ->
                AppListItem(
                    app = app,
                    onClick = { onNavigateTo(Destination.AppDetails(app.packageName)) }
                )
            }
        }
    }
}

@Composable
private fun AppFiles(files: List<PlayFile>) {
    val context = LocalContext.current
    SectionHeader(title = stringResource(R.string.details_more_files))
    files.forEach { file ->
        Info(
            title = AnnotatedString(text = file.name),
            description = AnnotatedString(
                text = Formatter.formatShortFileSize(context, file.size)
            )
        )
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview
@Composable
private fun MoreScreenPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    ScreenContent(app = app)
}
