/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aurora.extensions.toast
import com.aurora.store.R
import com.aurora.store.compose.composable.MicroG
import com.aurora.store.compose.composable.ScrollHint
import com.aurora.store.compose.preview.ThemePreviewProvider
import com.aurora.store.data.model.PermissionType
import com.aurora.store.data.providers.PermissionProvider
import com.aurora.store.viewmodel.onboarding.MicroGUIState
import com.aurora.store.viewmodel.onboarding.MicroGViewModel

@Composable
fun MicroGPage(
    onMicrogTOSChecked: (Boolean) -> Unit = {},
    viewModel: MicroGViewModel = hiltViewModel()
) {
    ScreenContent(
        uiState = viewModel.uiState,
        onInstall = { viewModel.downloadMicroG() },
        onRetry = { viewModel.retryDownload() },
        onMicrogTOSChecked = onMicrogTOSChecked
    )
}

@Composable
private fun ScreenContent(
    uiState: MicroGUIState,
    onInstall: () -> Unit = {},
    onRetry: () -> Unit = {},
    onMicrogTOSChecked: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = dimensionResource(R.dimen.padding_medium)),
            state = listState
        ) {
            item {
                Column(
                    modifier = Modifier.padding(dimensionResource(R.dimen.padding_medium))
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_title_gsf),
                        style = MaterialTheme.typography.headlineLargeEmphasized,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.onboarding_title_gsf_desc),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                MicroG(
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
                    uiState = uiState,
                    onTOSChecked = onMicrogTOSChecked
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

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun MicroGPagePreview() {
    ScreenContent(
        uiState = MicroGUIState()
    )
}
