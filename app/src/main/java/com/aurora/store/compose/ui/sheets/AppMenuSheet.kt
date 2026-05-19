/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.sheets

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aurora.extensions.openInfo
import com.aurora.extensions.toast
import com.aurora.store.AuroraApp
import com.aurora.store.R
import com.aurora.store.data.event.BusEvent
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.data.model.MinimalApp
import com.aurora.store.util.PackageUtil
import com.aurora.store.viewmodel.sheets.AppMenuViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppMenuSheet(
    app: MinimalApp,
    onDismiss: () -> Unit,
    viewModel: AppMenuViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isBlacklisted = viewModel.blacklistProvider.isBlacklisted(app.packageName)
    val isInstalled = PackageUtil.isInstalled(context, app.packageName)

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            viewModel.copyInstalledApp(context, app, uri)
        } else {
            context.toast(R.string.failed_apk_export)
        }
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        NavigationDrawerItem(
            icon = {
                Icon(
                    painter = painterResource(
                        if (isBlacklisted) R.drawable.ic_cancel else R.drawable.ic_blacklist
                    ),
                    contentDescription = null
                )
            },
            label = {
                val labelRes = if (isBlacklisted) {
                    R.string.action_whitelist
                } else {
                    R.string.action_blacklist_add
                }
                Text(stringResource(labelRes))
            },
            selected = false,
            onClick = {
                if (isBlacklisted) {
                    viewModel.blacklistProvider.whitelist(app.packageName)
                    context.toast(R.string.toast_apk_whitelisted)
                } else {
                    viewModel.blacklistProvider.blacklist(app.packageName)
                    context.toast(R.string.toast_apk_blacklisted)
                }
                AuroraApp.events.send(BusEvent.Blacklisted(app.packageName))
                onDismiss()
            },
            colors = NavigationDrawerItemDefaults.colors(
                unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent
            )
        )
        HorizontalDivider()
        if (isInstalled) {
            NavigationDrawerItem(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete_forever),
                        contentDescription = null
                    )
                },
                label = { Text(stringResource(R.string.action_uninstall)) },
                selected = false,
                onClick = {
                    AppInstaller.uninstall(context, app.packageName)
                    onDismiss()
                },
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
            NavigationDrawerItem(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_file_copy),
                        contentDescription = null
                    )
                },
                label = { Text(stringResource(R.string.action_export)) },
                selected = false,
                onClick = { exportLauncher.launch("${app.packageName}.zip") },
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
        }
        NavigationDrawerItem(
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_about),
                    contentDescription = null
                )
            },
            label = { Text(stringResource(R.string.action_info)) },
            selected = false,
            onClick = {
                context.openInfo(app.packageName)
                onDismiss()
            },
            colors = NavigationDrawerItemDefaults.colors(
                unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent
            )
        )
        HorizontalDivider()
        androidx.compose.foundation.layout.Spacer(Modifier.navigationBarsPadding())
    }
}
