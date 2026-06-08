/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.tv.ui.details

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import com.aurora.store.R
import com.aurora.store.data.model.AppState

enum class InstallLabel { INSTALL, CANCEL, INSTALLING, OPEN, UPDATE }

fun installButtonLabel(state: AppState): InstallLabel = when (state) {
    is AppState.Installed -> InstallLabel.OPEN
    is AppState.Updatable -> InstallLabel.UPDATE
    is AppState.Installing, is AppState.Verifying, is AppState.Purchasing -> InstallLabel.INSTALLING
    is AppState.Downloading, is AppState.Queued -> InstallLabel.CANCEL
    else -> InstallLabel.INSTALL
}

@Composable
fun InstallButton(
    state: AppState,
    onInstall: () -> Unit,
    onCancel: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = installButtonLabel(state)
    val text = when (label) {
        InstallLabel.INSTALL -> stringResource(R.string.action_install)
        InstallLabel.CANCEL -> stringResource(R.string.action_cancel)
        InstallLabel.INSTALLING -> stringResource(R.string.action_installing)
        InstallLabel.OPEN -> stringResource(R.string.action_open)
        InstallLabel.UPDATE -> stringResource(R.string.action_update)
    }
    Button(
        onClick = {
            when (label) {
                InstallLabel.CANCEL -> onCancel()
                InstallLabel.OPEN -> onOpen()
                // In-progress and non-cancellable (installing/verifying/purchasing): ignore clicks.
                InstallLabel.INSTALLING -> Unit
                else -> onInstall()
            }
        },
        modifier = modifier
    ) { Text(text) }
}
