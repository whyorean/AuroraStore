/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aurora.extensions.toast
import com.aurora.store.R
import com.aurora.store.compose.composable.MicroG
import com.aurora.store.compose.preview.PreviewTemplate
import com.aurora.store.data.model.PermissionType
import com.aurora.store.data.providers.PermissionProvider
import com.aurora.store.viewmodel.onboarding.MicroGUIState
import com.aurora.store.viewmodel.onboarding.MicroGViewModel

@Composable
fun MicroGPage(
    onCheckedChange: (Boolean) -> Unit = {},
    viewModel: MicroGViewModel = hiltViewModel(),
) {
    ScreenContent(
        uiState = viewModel.uiState,
        onInstall = { viewModel.downloadMicroG() },
        onMicrogTOCChange = onCheckedChange,
    )
}

@Composable
private fun ScreenContent(
    uiState: MicroGUIState,
    onInstall: () -> Unit = {},
    onMicrogTOCChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimensionResource(R.dimen.padding_medium))
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_medium))
    ) {
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
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        MicroG(
            onInstall = {
                when {
                    PermissionProvider.isGranted(context, PermissionType.INSTALL_UNKNOWN_APPS) -> {
                        onInstall()
                    }

                    else                                                                       -> context.toast(
                        R.string.permissions_denied
                    )
                }
            },
            uiState = uiState,
            onCheckedChange = onMicrogTOCChange
        )
    }

}

@Preview(showBackground = true)
@Composable
private fun MicroGPagePreview() {
    PreviewTemplate {
        ScreenContent(
            uiState = MicroGUIState(),
        )
    }
}
