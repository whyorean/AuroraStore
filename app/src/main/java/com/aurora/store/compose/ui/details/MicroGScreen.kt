/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurora.extensions.adaptiveNavigationIcon
import com.aurora.extensions.isWindowCompact
import com.aurora.extensions.toast
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.compose.composable.MicroG
import com.aurora.store.compose.composable.ScrollHint
import com.aurora.store.compose.composable.TopAppBar
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.ThemePreviewProvider
import com.aurora.store.data.model.PermissionType
import com.aurora.store.data.providers.PermissionProvider
import com.aurora.store.viewmodel.details.AppDetailsViewModel
import com.aurora.store.viewmodel.onboarding.MicroGUIState
import com.aurora.store.viewmodel.onboarding.MicroGViewModel

@Composable
fun MicroGScreen(
    packageName: String,
    onNavigateUp: () -> Unit,
    onIgnore: (Boolean) -> Unit,
    appDetailsViewModel: AppDetailsViewModel = hiltViewModel(key = packageName),
    viewModel: MicroGViewModel = hiltViewModel(),
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo()
) {
    val app by appDetailsViewModel.app.collectAsStateWithLifecycle()
    val topAppBarTitle = when {
        windowAdaptiveInfo.isWindowCompact -> app!!.displayName
        else -> stringResource(R.string.onboarding_title_gsf)
    }

    ScreenContent(
        topAppBarTitle = topAppBarTitle,
        uiState = viewModel.uiState,
        onNavigateUp = onNavigateUp,
        onInstall = { viewModel.downloadMicroG() },
        onRetry = { viewModel.retryDownload() },
        onIgnore = onIgnore
    )
}

@Composable
private fun ScreenContent(
    topAppBarTitle: String? = null,
    uiState: MicroGUIState = MicroGUIState(),
    onNavigateUp: () -> Unit = {},
    onInstall: () -> Unit = {},
    onRetry: () -> Unit = {},
    onIgnore: (Boolean) -> Unit = {},
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo()
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = topAppBarTitle,
                navigationIcon = windowAdaptiveInfo.adaptiveNavigationIcon,
                onNavigateUp = onNavigateUp
            )
        },
        bottomBar = {
            Surface(shadowElevation = 4.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = dimensionResource(R.dimen.padding_medium))
                        .padding(vertical = dimensionResource(R.dimen.padding_small)),
                    horizontalArrangement = Arrangement.spacedBy(
                        dimensionResource(R.dimen.padding_medium)
                    )
                ) {
                    FilledTonalButton(
                        modifier = Modifier.weight(1F),
                        onClick = onNavigateUp
                    ) {
                        Text(
                            text = stringResource(R.string.action_cancel),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        modifier = Modifier.weight(1F),
                        onClick = { onIgnore(uiState.isInstalled) },
                        enabled = !uiState.isInProgress
                    ) {
                        Text(
                            text = if (uiState.isInstalled) {
                                stringResource(R.string.action_install)
                            } else {
                                stringResource(R.string.action_ignore)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(horizontal = dimensionResource(R.dimen.padding_medium)),
                state = listState
            ) {
                item {
                    MicroG(
                        modifier = Modifier.fillMaxWidth(),
                        onInstall = {
                            when {
                                PermissionProvider.isGranted(
                                    context,
                                    PermissionType.INSTALL_UNKNOWN_APPS
                                ) -> {
                                    onInstall()
                                }

                                else -> context.toast(R.string.permissions_denied)
                            }
                        },
                        onRetry = onRetry,
                        uiState = uiState
                    )
                }
            }

            ScrollHint(
                listState = listState,
                bottomPadding = 5.dp,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview
@Composable
private fun MicroGScreenPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    ScreenContent(topAppBarTitle = app.displayName)
}
