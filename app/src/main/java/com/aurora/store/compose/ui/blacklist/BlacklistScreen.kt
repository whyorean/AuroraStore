/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.blacklist

import android.content.pm.PackageInfo
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.pm.PackageInfoCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurora.Constants
import com.aurora.extensions.toast
import com.aurora.store.R
import com.aurora.store.compose.composable.BlackListItem
import com.aurora.store.compose.ui.blacklist.menu.BlacklistMenu
import com.aurora.store.compose.ui.blacklist.menu.MenuItem
import com.aurora.store.util.PackageUtil
import com.aurora.store.viewmodel.blacklist.BlacklistViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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
    val textFieldState = rememberTextFieldState()
    val searchBarState = rememberSearchBarState()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

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

    LaunchedEffect(key1 = textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .collectLatest { query -> onSearch(query) }
    }

    @Composable
    fun SetupMenu() {
        BlacklistMenu { menuItem ->
            when (menuItem) {
                MenuItem.SELECT_ALL -> onBlacklistAll()
                MenuItem.REMOVE_ALL -> onWhitelistAll()
                MenuItem.IMPORT -> {
                    docImportLauncher.launch(arrayOf(Constants.JSON_MIME_TYPE))
                }

                MenuItem.EXPORT -> {
                    docExportLauncher.launch(
                        "aurora_store_apps_${Calendar.getInstance().time.time}.json"
                    )
                }
            }
        }
    }

    fun onRequestSearch(query: String) {
        textFieldState.setTextAndPlaceCursorAtEnd(query.trim())
        coroutineScope.launch { searchBarState.animateToCollapsed() }
        onSearch(textFieldState.text.toString())
    }

    @Composable
    fun SearchBar() {
        val inputField = @Composable {
            SearchBarDefaults.InputField(
                modifier = Modifier.focusRequester(focusRequester),
                searchBarState = searchBarState,
                textFieldState = textFieldState,
                onSearch = { query -> onRequestSearch(query) },
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_hint),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_round_search),
                        contentDescription = stringResource(R.string.action_search)
                    )
                },
                trailingIcon = {
                    if (textFieldState.text.isNotBlank()) {
                        IconButton(
                            onClick = {
                                textFieldState.clearText()
                                focusRequester.requestFocus()
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_cancel),
                                contentDescription = stringResource(R.string.action_clear)
                            )
                        }
                    }
                }
            )
        }

        AppBarWithSearch(
            state = searchBarState,
            inputField = inputField,
            navigationIcon = {
                IconButton(onClick = onNavigateUp) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.action_back)
                    )
                }
            },
            actions = { if (!packages.isNullOrEmpty()) SetupMenu() }
        )
    }

    Scaffold(topBar = { SearchBar() }) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_xxsmall))
        ) {
            items(items = packages ?: emptyList(), key = { p -> p.packageName.hashCode() }) { pkg ->
                val isBlacklisted = isPackageBlacklisted(pkg.packageName)
                val isFiltered = isPackageFiltered(pkg)
                BlackListItem(
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
