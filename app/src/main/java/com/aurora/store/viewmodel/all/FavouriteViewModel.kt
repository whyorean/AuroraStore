/*
 * SPDX-FileCopyrightText: 2024-2025 Rahul Kumar Patel <whyorean@gmail.com>
 * SPDX-FileCopyrightText: 2024-2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.viewmodel.all

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.aurora.extensions.TAG
import com.aurora.store.data.paging.GenericPagingSource.Companion.pager
import com.aurora.store.data.room.favourite.Favourite
import com.aurora.store.data.room.favourite.FavouriteDao
import com.aurora.store.data.room.favourite.ImportExport
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class FavouriteViewModel @Inject constructor(
    private val favouriteDao: FavouriteDao,
    private val json: Json,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _favourites = MutableStateFlow<PagingData<Favourite>>(PagingData.empty())
    val favourites = _favourites.asStateFlow()

    init {
        getPagedFavourites()
    }

    fun importFavourites(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use {
                    val importExport = json.decodeFromString<ImportExport>(
                        it.bufferedReader().readText()
                    )

                    favouriteDao.insertAll(
                        importExport.favourites.map { fav ->
                            fav.copy(mode = Favourite.Mode.IMPORT)
                        }
                    )
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to import favourites", exception)
            }
        }
    }

    fun exportFavourites(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(
                        json.encodeToString(ImportExport(favouriteDao.favourites().first()))
                            .encodeToByteArray()
                    )
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to export favourites", exception)
            }
        }
    }

    fun removeFavourite(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            favouriteDao.delete(packageName)
        }
    }

    private fun getPagedFavourites() {
        pager { favouriteDao.pagedFavourites() }.flow
            .distinctUntilChanged()
            .cachedIn(viewModelScope)
            .onEach { _favourites.value = it }
            .launchIn(viewModelScope)
    }
}
