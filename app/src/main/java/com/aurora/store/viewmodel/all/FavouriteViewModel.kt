/*
 * Aurora Store
 *  Copyright (C) 2021, Rahul Kumar Patel <whyorean@gmail.com>
 *
 *  Aurora Store is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Aurora Store is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.aurora.store.viewmodel.all

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.store.data.room.favourite.Favourite
import com.aurora.store.data.room.favourite.FavouriteDao
import com.aurora.store.data.room.favourite.ImportExport
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavouriteViewModel @Inject constructor(
    private val favouriteDao: FavouriteDao,
    private val gson: Gson
) : ViewModel() {

    val favouritesList = favouriteDao.favourites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun importFavourites(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use {
                val importExport =
                    gson.fromJson(it.bufferedReader().readText(), ImportExport::class.java)
                favouriteDao.insertAll(
                    importExport.favourites.map { fav -> fav.copy(mode = Favourite.Mode.IMPORT) }
                )
            }
        }
    }

    fun exportFavourites(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri)?.use {
                it.write(gson.toJson(ImportExport(favouritesList.value!!)).encodeToByteArray())
            }
        }
    }

    fun removeFavourite(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            favouriteDao.delete(packageName)
        }
    }
}
