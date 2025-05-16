/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.commons

import android.content.pm.PackageInfo
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.pm.PackageInfoCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurora.Constants
import com.aurora.extensions.toast
import com.aurora.store.R
import com.aurora.store.compose.composables.BlackListComposable
import com.aurora.store.compose.composables.SearchAppBarComposable
import com.aurora.store.compose.menu.BlacklistMenu
import com.aurora.store.compose.menu.items.BlacklistMenuItem
import com.aurora.store.util.PackageUtil
import com.aurora.store.viewmodel.all.BlacklistViewModel
import java.util.Calendar

@Composable
fun BlacklistScreen(onNavigateUp: () -> Unit, viewModel: BlacklistViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val packages by viewModel.filteredPackages.collectAsStateWithLifecycle()

    ScreenContent(
        packages = packages,
        onNavigateUp = onNavigateUp,
        isPackageBlacklisted = { pkgName -> pkgName in viewModel.blacklist },
        isPackageFiltered = { pkgInfo -> viewModel.isFiltered(pkgInfo) },
        onBlacklistImport = { uri ->
            viewModel.importBlacklist(context, uri)
            context.toast(R.string.toast_black_import_success)
        },
        onBlacklistExport = { uri ->
            viewModel.exportBlacklist(context, uri)
            context.toast(R.string.toast_black_export_success)
        },
        onBlacklist = { packageName -> viewModel.blacklist(packageName) },
        onBlacklistAll = { viewModel.blacklistAll() },
        onWhitelist = { packageName -> viewModel.whitelist(packageName) },
        onWhitelistAll = { viewModel.whitelistAll() },
        onSearch = { query -> viewModel.search(query) }
    )
}

@Composable
private fun ScreenContent(
    packages: List<PackageInfo>? = null,
    onNavigateUp: () -> Unit = {},
    isPackageBlacklisted: (packageName: String) -> Boolean = { false },
    isPackageFiltered: (packageInfo: PackageInfo) -> Boolean = { false },
    onBlacklistImport: (uri: Uri) -> Unit = {},
    onBlacklistExport: (uri: Uri) -> Unit = {},
    onBlacklist: (packageName: String) -> Unit = {},
    onBlacklistAll: () -> Unit = {},
    onWhitelist: (packageName: String) -> Unit = {},
    onWhitelistAll: () -> Unit = {},
    onSearch: (query: String) -> Unit = {}
) {

    val context = LocalContext.current
    val docImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = {
            if (it != null) {
                onBlacklistImport(it)
            } else {
                context.toast(R.string.toast_black_import_failed)
            }
        }
    )
    val docExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(Constants.JSON_MIME_TYPE),
        onResult = {
            if (it != null) {
                onBlacklistExport(it)
            } else {
                context.toast(R.string.toast_black_export_failed)
            }
        }
    )

    @Composable
    fun SetupMenu() {
        BlacklistMenu { menuItem ->
            when (menuItem) {
                BlacklistMenuItem.SELECT_ALL -> onBlacklistAll()
                BlacklistMenuItem.REMOVE_ALL -> onWhitelistAll()
                BlacklistMenuItem.IMPORT -> {
                    docImportLauncher.launch(arrayOf(Constants.JSON_MIME_TYPE))
                }

                BlacklistMenuItem.EXPORT -> {
                    docExportLauncher.launch(
                        "aurora_store_apps_${Calendar.getInstance().time.time}.json"
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            SearchAppBarComposable(
                onNavigateUp = onNavigateUp,
                actions = { if (!packages.isNullOrEmpty()) SetupMenu() },
                searchHint = R.string.search_hint,
                onSearch = { query -> onSearch(query) }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_xxsmall))
        ) {
            items(items = packages ?: emptyList(), key = { p -> p.packageName.hashCode() }) { pkg ->
                val isBlacklisted = isPackageBlacklisted(pkg.packageName)
                val isFiltered = isPackageFiltered(pkg)
                BlackListComposable(
                    icon = PackageUtil.getIconForPackage(context, pkg.packageName)!!,
                    displayName = pkg.applicationInfo!!.loadLabel(context.packageManager).toString(),
                    packageName = pkg.packageName,
                    versionName = pkg.versionName!!,
                    versionCode = PackageInfoCompat.getLongVersionCode(pkg),
                    isChecked = isBlacklisted || isFiltered,
                    isEnabled = !isFiltered,
                    onClick = {
                        if (isBlacklisted) {
                            onWhitelist(pkg.packageName)
                        } else {
                            onBlacklist(pkg.packageName)
                        }
                    }
                )
            }
        }
    }
}

@Preview
@Composable
private fun BlacklistScreenPreview() {
    ScreenContent()
}
