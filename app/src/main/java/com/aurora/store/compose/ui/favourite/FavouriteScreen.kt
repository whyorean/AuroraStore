/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.favourite

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.aurora.Constants.JSON_MIME_TYPE
import com.aurora.extensions.emptyPagingItems
import com.aurora.extensions.toast
import com.aurora.store.R
import com.aurora.store.compose.composable.ContainedLoadingIndicator
import com.aurora.store.compose.composable.Error
import com.aurora.store.compose.composable.FavouriteListItem
import com.aurora.store.compose.composable.TopAppBar
import com.aurora.store.compose.preview.FavouritePreviewProvider
import com.aurora.store.compose.preview.PreviewTemplate
import com.aurora.store.compose.ui.favourite.menu.FavouriteMenu
import com.aurora.store.compose.ui.favourite.menu.MenuItem
import com.aurora.store.data.room.favourite.Favourite
import com.aurora.store.viewmodel.all.FavouriteViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Calendar
import kotlin.random.Random

@Composable
fun FavouriteScreen(
    onNavigateUp: () -> Unit,
    onNavigateToAppDetails: (packageName: String) -> Unit,
    viewModel: FavouriteViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val favourites = viewModel.favourites.collectAsLazyPagingItems()

    val documentOpenLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = {
            if (it != null) {
                viewModel.importFavourites(it)
                context.toast(R.string.toast_fav_import_success)
            } else {
                context.toast(R.string.toast_fav_import_failed)
            }
        }
    )

    val documentCreateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(JSON_MIME_TYPE),
        onResult = {
            if (it != null) {
                viewModel.exportFavourites(it)
                context.toast(R.string.toast_fav_export_success)
            } else {
                context.toast(R.string.toast_fav_export_failed)
            }
        }
    )

    ScreenContent(
        favourites = favourites,
        onNavigateUp = onNavigateUp,
        onNavigateToAppDetails = onNavigateToAppDetails,
        onRemoveFavourite = { packageName -> viewModel.removeFavourite(packageName) },
        onImportFavourites = {
            documentOpenLauncher.launch(arrayOf(JSON_MIME_TYPE))
        },
        onExportFavourites = {
            documentCreateLauncher.launch(
                "aurora_store_favourites_${Calendar.getInstance().time.time}.json"
            )
        }
    )
}

@Composable
private fun ScreenContent(
    favourites: LazyPagingItems<Favourite> = emptyPagingItems(),
    onNavigateUp: () -> Unit = {},
    onNavigateToAppDetails: (packageName: String) -> Unit = {},
    onRemoveFavourite: (packageName: String) -> Unit = {},
    onImportFavourites: () -> Unit = {},
    onExportFavourites: () -> Unit = {}
) {
    /*
     * For some reason paging3 frequently out-of-nowhere invalidates the list which causes
     * the loading animation to play again even if the keys are same causing a glitching effect.
     *
     * Save the initial loading state to make sure we don't replay the loading animation again.
     */
    var initialLoad by rememberSaveable { mutableStateOf(true) }

    @Composable
    fun SetupMenu() {
        FavouriteMenu { menuItem ->
            when (menuItem) {
                MenuItem.IMPORT -> onImportFavourites()
                MenuItem.EXPORT -> onExportFavourites()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.title_favourites_manager),
                onNavigateUp = onNavigateUp,
                actions = { if (favourites.itemCount != 0) SetupMenu() }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(vertical = dimensionResource(R.dimen.padding_medium))
        ) {
            when {
                favourites.loadState.refresh is LoadState.Loading && initialLoad -> {
                    ContainedLoadingIndicator()
                }

                else -> {
                    initialLoad = false

                    if (favourites.itemCount == 0) {
                        Error(
                            modifier = Modifier.padding(paddingValues),
                            painter = painterResource(R.drawable.ic_favorite_unchecked),
                            message = stringResource(R.string.details_no_favourites)
                        )
                    } else {
                        LazyColumn {
                            items(
                                count = favourites.itemCount,
                                key = favourites.itemKey { it.packageName }
                            ) { index ->
                                favourites[index]?.let { favourite ->
                                    FavouriteListItem(
                                        modifier = Modifier.animateItem(),
                                        favourite = favourite,
                                        onClick = { onNavigateToAppDetails(favourite.packageName) },
                                        onClear = { onRemoveFavourite(favourite.packageName) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun FavouriteScreenPreview(
    @PreviewParameter(FavouritePreviewProvider ::class) favourite: Favourite
) {
    PreviewTemplate {
        val favourites = List(10) {
            favourite.copy(packageName = "${favourite.packageName}.${Random.nextInt()}")
        }
        val flow = MutableStateFlow(PagingData.from(favourites)).collectAsLazyPagingItems()

        ScreenContent(favourites = flow)
    }
}
