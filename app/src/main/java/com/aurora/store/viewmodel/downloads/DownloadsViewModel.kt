/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.viewmodel.downloads

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.aurora.extensions.TAG
import com.aurora.store.data.helper.DownloadHelper
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.data.paging.GenericPagingSource.Companion.pager
import com.aurora.store.data.room.download.Download
import com.aurora.store.data.work.ExportWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadHelper: DownloadHelper,
    private val appInstaller: AppInstaller,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _downloads = MutableStateFlow<PagingData<Download>>(PagingData.empty())
    val downloads = _downloads.asStateFlow()

    init {
        getPagedDownloads()
    }

    fun cancel(packageName: String) {
        viewModelScope.launch(NonCancellable) {
            downloadHelper.cancelDownload(packageName)
        }
    }

    fun cancelAll() {
        viewModelScope.launch(NonCancellable) {
            downloadHelper.cancelAll()
        }
    }

    fun clear(packageName: String, versionCode: Long) {
        viewModelScope.launch(NonCancellable) {
            downloadHelper.clearDownload(packageName, versionCode)
        }
    }

    fun clearFinished() {
        viewModelScope.launch(NonCancellable) {
            downloadHelper.clearFinishedDownloads()
        }
    }

    fun clearAll() {
        viewModelScope.launch(NonCancellable) {
            downloadHelper.clearAllDownloads()
        }
    }

    fun install(download: Download) {
        try {
            appInstaller.getPreferredInstaller().install(download)
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to install ${download.packageName}", exception)
        }
    }

    fun export(download: Download, uri: Uri) {
        ExportWorker.exportDownloadedApp(context, download, uri)
    }

    private fun getPagedDownloads() {
        pager { downloadHelper.pagedDownloads }.flow
            .distinctUntilChanged()
            .cachedIn(viewModelScope)
            .onEach { _downloads.value = it }
            .launchIn(viewModelScope)
    }
}
