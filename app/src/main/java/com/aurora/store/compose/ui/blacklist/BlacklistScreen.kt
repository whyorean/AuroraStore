/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.blacklist

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurora.Constants
import com.aurora.extensions.toast
import com.aurora.store.R
import com.aurora.store.compose.composable.BlackListItem
import com.aurora.store.compose.composable.ContainedLoadingIndicator
import com.aurora.store.compose.composable.ScrollHint
import com.aurora.store.compose.composable.TextDividerComposable
import com.aurora.store.compose.preview.ThemePreviewProvider
import com.aurora.store.compose.ui.blacklist.menu.BlacklistMenu
import com.aurora.store.compose.ui.blacklist.menu.MenuItem
import com.aurora.store.data.model.BlacklistAppItem
import com.aurora.store.viewmodel.blacklist.BlacklistViewModel
import java.util.Calendar
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun BlacklistScreen(viewModel: BlacklistViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val packages by viewModel.filteredPackages.collectAsStateWithLifecycle()

    ScreenContent(
        packages = packages,
        isPackageBlacklisted = { pkgName -> pkgName in viewModel.blacklist },
        onBlacklistImport = { uri ->
            viewModel.importBlacklist(uri)
            context.toast(R.string.toast_black_import_success)
        },
        onBlacklistExport = { uri ->
            viewModel.exportBlacklist(uri)
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
    packages: List<BlacklistAppItem>? = null,
    isPackageBlacklisted: (packageName: String) -> Boolean = { false },
    onBlacklistImport: (uri: Uri) -> Unit = {},
    onBlacklistExport: (uri: Uri) -> Unit = {},
    onBlacklist: (packageName: String) -> Unit = {},
    onBlacklistAll: () -> Unit = {},
    onWhitelist: (packageName: String) -> Unit = {},
    onWhitelistAll: () -> Unit = {},
    onSearch: (query: String) -> Unit = {}
) {
    val activity = LocalActivity.current as? ComponentActivity
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
        val interactionSource = remember { MutableInteractionSource() }

        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collectLatest { interaction ->
                if (interaction is PressInteraction.Press) {
                    awaitFrame()
                    focusRequester.requestFocus()
                }
            }
        }

        val inputField = @Composable {
            SearchBarDefaults.InputField(
                modifier = Modifier.focusRequester(focusRequester),
                searchBarState = searchBarState,
                textFieldState = textFieldState,
                interactionSource = interactionSource,
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
                IconButton(onClick = { activity?.onBackPressedDispatcher?.onBackPressed() }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.action_back)
                    )
                }
            },
            actions = { SetupMenu() },
            colors = SearchBarDefaults.appBarWithSearchColors(
                appBarContainerColor = Color.Transparent
            )
        )
        ExpandedFullScreenSearchBar(state = searchBarState, inputField = inputField) {
            if (textFieldState.text.length >= 3) {
                packages?.take(10)?.forEach { pkg ->
                    Text(
                        text = pkg.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onRequestSearch(pkg.displayName) }
                            .padding(
                                horizontal = dimensionResource(R.dimen.padding_medium),
                                vertical = dimensionResource(R.dimen.padding_small)
                            )
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = { SearchBar() }
    ) { paddingValues ->
        val listState = rememberLazyListState()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (packages == null) {
                ContainedLoadingIndicator()
            } else {
                val (selectedPackages, otherPackages) = packages.partition { pkg ->
                    isPackageBlacklisted(pkg.packageName) || pkg.isFiltered
                }

                @Composable
                fun BlacklistRow(pkg: BlacklistAppItem) {
                    val isBlacklisted = isPackageBlacklisted(pkg.packageName)
                    BlackListItem(
                        icon = pkg.icon,
                        displayName = pkg.displayName,
                        packageName = pkg.packageName,
                        versionName = pkg.versionName,
                        versionCode = pkg.versionCode,
                        isChecked = isBlacklisted || pkg.isFiltered,
                        isEnabled = !pkg.isFiltered,
                        onClick = {
                            if (isBlacklisted) {
                                onWhitelist(pkg.packageName)
                            } else {
                                onBlacklist(pkg.packageName)
                            }
                        }
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(
                        dimensionResource(R.dimen.margin_xxsmall)
                    )
                ) {
                    if (selectedPackages.isNotEmpty()) {
                        stickyHeader(key = "header_selected") {
                            Surface(modifier = Modifier.fillMaxWidth()) {
                                TextDividerComposable(
                                    title = stringResource(
                                        R.string.header_blacklist_selected
                                    )
                                )
                            }
                        }
                        items(items = selectedPackages, key = { p ->
                            p.packageName.hashCode()
                        }) { pkg -> BlacklistRow(pkg) }
                    }

                    if (otherPackages.isNotEmpty()) {
                        stickyHeader(key = "header_others") {
                            Surface(modifier = Modifier.fillMaxWidth()) {
                                TextDividerComposable(
                                    title = stringResource(
                                        R.string.header_blacklist_others
                                    )
                                )
                            }
                        }
                        items(items = otherPackages, key = { p ->
                            p.packageName.hashCode()
                        }) { pkg -> BlacklistRow(pkg) }
                    }
                }
                ScrollHint(
                    listState = listState,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview
@Composable
private fun BlacklistScreenPreview() {
    ScreenContent()
}
