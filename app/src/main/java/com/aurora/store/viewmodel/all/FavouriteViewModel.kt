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
import androidx.paging.filter
import com.aurora.extensions.TAG
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.store.AuroraApp
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.data.helper.DownloadHelper
import com.aurora.store.data.paging.GenericPagingSource.Companion.pager
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.data.providers.WhitelistProvider
import com.aurora.store.data.room.favourite.Favourite
import com.aurora.store.data.room.favourite.FavouriteDao
import com.aurora.store.data.room.favourite.ImportExport
import com.aurora.store.util.PackageUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@HiltViewModel
class FavouriteViewModel @Inject constructor(
    private val favouriteDao: FavouriteDao,
    private val json: Json,
    private val appDetailsHelper: AppDetailsHelper,
    private val downloadHelper: DownloadHelper,
    private val authProvider: AuthProvider,
    @ApplicationContext private val context: Context,
    private val whitelistProvider: WhitelistProvider
) : ViewModel() {

    private val _favourites = MutableStateFlow<PagingData<Favourite>>(PagingData.empty())
    val favourites = _favourites.asStateFlow()

    val downloadsList get() = downloadHelper.downloadsList

    private val _isEnqueuing = MutableStateFlow(false)
    val isEnqueuing = _isEnqueuing.asStateFlow()

    // Emits the number of favourites actually enqueued for install (0 when nothing was
    // applicable, -1 when fetching details failed) so the screen can show accurate feedback.
    private val _enqueueResult = MutableSharedFlow<Int>()
    val enqueueResult = _enqueueResult.asSharedFlow()

    // Whether at least one favourite is still not installed, recomputed whenever the favourites
    // list changes or any app is installed/removed. Drives visibility of the "Install all"
    // action so it hides once every favourite is already installed. Defaults to true to avoid
    // briefly hiding the action before the first check completes.
    val hasInstallableFavourites = combine(
        favouriteDao.favourites(),
        AuroraApp.events.installerEvent
            .filter { it is InstallerEvent.Installed || it is InstallerEvent.Uninstalled }
            .map { }
            .onStart { emit(Unit) }
    ) { favourites, _ ->
        favourites.any { !PackageUtil.isInstalled(context, it.packageName) }
    }.flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

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

    /**
     * Fetches details for all favourites and enqueues the installable ones for download &
     * install. Already installed (and up-to-date) apps are skipped, as are paid apps that an
     * anonymous account can't acquire. The actual count enqueued is emitted via [enqueueResult].
     */
    fun installAll() {
        viewModelScope.launch(Dispatchers.IO) {
            _isEnqueuing.value = true
            try {
                val packageNames = favouriteDao.favourites().first().map { it.packageName }
                val installable = appDetailsHelper.getAppByPackageName(packageNames).filter { app ->
                    val needsInstall = !PackageUtil.isInstalled(context, app.packageName) ||
                        PackageUtil.isUpdatable(context, app.packageName, app.versionCode)
                    val acquirable = app.isFree || !authProvider.isAnonymous
                    needsInstall && acquirable
                }

                installable.forEach { downloadHelper.enqueueApp(it) }
                _enqueueResult.emit(installable.size)
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to enqueue favourites for install", exception)
                _enqueueResult.emit(-1)
            } finally {
                _isEnqueuing.value = false
            }
        }
    }

    private fun getPagedFavourites() {
        pager { favouriteDao.pagedFavourites() }.flow
            .map { it.filter { whitelistProvider.isWhitelisted(it.packageName) } }
            .distinctUntilChanged()
            .cachedIn(viewModelScope)
            .onEach { _favourites.value = it }
            .launchIn(viewModelScope)
    }
}
