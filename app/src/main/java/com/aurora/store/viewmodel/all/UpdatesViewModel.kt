/*
 * SPDX-FileCopyrightText: 2021 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.viewmodel.all

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.extensions.TAG
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.store.data.ExodusRepository
import com.aurora.store.data.helper.DownloadHelper
import com.aurora.store.data.helper.UpdateHelper
import com.aurora.store.data.model.DownloadStatus
import com.aurora.gplayapi.data.models.App
import com.aurora.store.data.model.ExodusTracker
import com.aurora.store.data.model.StorageRequirement
import com.aurora.store.data.room.update.Update
import com.aurora.store.util.StorageUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

@HiltViewModel
class UpdatesViewModel @Inject constructor(
    val updateHelper: UpdateHelper,
    private val downloadHelper: DownloadHelper,
    private val appDetailsHelper: AppDetailsHelper,
    private val exodusRepository: ExodusRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    var updateAllEnqueued: Boolean = false

    private val _storageWarning = MutableSharedFlow<StorageRequirement>()
    val storageWarning = _storageWarning.asSharedFlow()

    private val _cancellingPackages = MutableStateFlow<Set<String>>(emptySet())
    val cancellingPackages = _cancellingPackages.asStateFlow()

    val downloadsList get() = downloadHelper.downloadsList
    val updates get() = updateHelper.updates
    val ignoredUpdates get() = updateHelper.ignoredUpdates

    val fetchingUpdates = updateHelper.isCheckingUpdates

    fun fetchUpdates() {
        updateHelper.checkUpdatesNow()
    }

    fun unignore(packageName: String) {
        viewModelScope.launch { updateHelper.unignore(packageName) }
    }

    fun download(update: Update) {
        viewModelScope.launch {
            if (hasSpaceFor(listOf(update), update.displayName)) {
                downloadHelper.enqueueUpdate(update)
            }
        }
    }

    fun downloadApp(app: App, onDetailsFallback: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            // Discovery cards can carry a partial App model even when some file URLs are
            // present. Resolve the same complete model used by the details page before
            // enqueueing, so split APKs, dependencies, and version metadata stay consistent.
            val downloadableApp = try {
                appDetailsHelper.getAppByPackageName(app.packageName)
                    .copy(isInstalled = app.isInstalled)
            } catch (exception: Exception) {
                Log.e(TAG, "Could not load install files for ${app.packageName}", exception)
                onDetailsFallback()
                return@launch
            }
            if (hasSpaceFor(downloadableApp)) {
                downloadHelper.enqueueApp(downloadableApp)
            }
        }
    }

    suspend fun getNewTrackers(
        packageName: String,
        installedVersionCode: Long
    ): List<ExodusTracker> = exodusRepository.getNewTrackers(packageName, installedVersionCode)

    fun downloadAll(updates: List<Update>) {
        viewModelScope.launch {
            if (hasSpaceFor(updates)) updates.forEach { downloadHelper.enqueueUpdate(it) }
        }
    }

    private suspend fun hasSpaceFor(updates: List<Update>, appName: String? = null): Boolean {
        val sizes = updates
            .filter { downloadHelper.needsDownload(it.packageName, it.versionCode) }
            .map { it.size }

        val requirement = StorageUtil.check(context, sizes, appName)
        if (!requirement.isSufficient) _storageWarning.emit(requirement)
        return requirement.isSufficient
    }

    private suspend fun hasSpaceFor(app: App): Boolean {
        if (!downloadHelper.needsDownload(app.packageName, app.versionCode)) return true

        val requirement = StorageUtil.check(context, listOf(app.size), app.displayName)
        if (!requirement.isSufficient) _storageWarning.emit(requirement)
        return requirement.isSufficient
    }

    fun cancelDownload(packageName: String) {
        viewModelScope.launch {
            if (packageName in _cancellingPackages.value) return@launch
            _cancellingPackages.update { it + packageName }
            try {
                downloadHelper.cancelDownload(packageName)
                downloadHelper.downloadsList.first { downloads ->
                    downloads.none { download ->
                        download.packageName == packageName && download.status in setOf(
                            DownloadStatus.QUEUED,
                            DownloadStatus.PURCHASING,
                            DownloadStatus.DOWNLOADING,
                            DownloadStatus.VERIFYING
                        )
                    }
                }
            } finally {
                _cancellingPackages.update { it - packageName }
            }
        }
    }

    fun cancelAll() {
        viewModelScope.launch { downloadHelper.cancelAll(true) }
    }
}
