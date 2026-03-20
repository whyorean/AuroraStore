/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.util.fastForEach
import com.aurora.extensions.browse
import com.aurora.store.R
import com.aurora.store.compose.preview.PreviewTemplate
import com.aurora.store.data.model.Link
import com.aurora.store.viewmodel.onboarding.MicroGUIState

/**
 * Composable to display suggestion to install microG
 * @param modifier Modifier for the composable
 * @param onInstall Callback when user requests installing microG bundle
 */
@Composable
fun MicroG(
    modifier: Modifier = Modifier,
    uiState: MicroGUIState,
    onInstall: () -> Unit = {},
    onTOSChecked: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    var isChecked by rememberSaveable { mutableStateOf(false) }
    val links = listOf(
        Link(
            id = 2,
            title = stringResource(R.string.details_dev_website),
            subtitle = stringResource(R.string.microg_website),
            icon = R.drawable.ic_network,
            url = "https://microG.org"
        ),
        Link(
            id = 4,
            title = stringResource(R.string.privacy_policy_title),
            subtitle = stringResource(R.string.microg_privacy_policy),
            icon = R.drawable.ic_privacy,
            url = "https://microg.org/privacy.html"
        ),
        Link(
            id = 5,
            title = stringResource(R.string.menu_disclaimer),
            subtitle = stringResource(R.string.microg_license_agreement),
            icon = R.drawable.ic_disclaimer,
            url = "https://raw.githubusercontent.com/microg/GmsCore/refs/heads/master/LICENSE"
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                horizontal = dimensionResource(R.dimen.padding_small),
                vertical = dimensionResource(R.dimen.padding_xxsmall)
            ),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_xxsmall))
        ) {
            Text(
                text = stringResource(R.string.onboarding_gms_microg),
                style = MaterialTheme.typography.bodyMedium
            )

            links.fastForEach { link ->
                LinkListItem(
                    link = link,
                    onClick = { context.browse(link.url) },
                    iconTint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = dimensionResource(R.dimen.padding_xlarge)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_small))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    dimensionResource(R.dimen.padding_small)
                )
            ) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = {
                        isChecked = it
                        onTOSChecked(it)
                    },
                    enabled = !uiState.isInstalled && !uiState.isDownloading
                )
                Text(
                    text = stringResource(R.string.onboarding_gms_agreement),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Button(
                onClick = onInstall,
                enabled = isChecked && !uiState.isDownloading && !uiState.isInstalled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (uiState.isDownloading) {
                        stringResource(R.string.action_installing)
                    } else {
                        stringResource(R.string.action_install_microG)
                    }
                )
            }
            if (uiState.isDownloading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MicroGPreview() {
    PreviewTemplate {
        MicroG(uiState = MicroGUIState())
    }
}
