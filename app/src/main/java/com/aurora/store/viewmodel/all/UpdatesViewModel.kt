/*
 * SPDX-FileCopyrightText: 2021 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.viewmodel.all

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.store.data.ExodusRepository
import com.aurora.store.data.helper.DownloadHelper
import com.aurora.store.data.helper.UpdateHelper
import com.aurora.store.data.model.ExodusTracker
import com.aurora.store.data.model.StorageRequirement
import com.aurora.store.data.room.update.Update
import com.aurora.store.util.StorageUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

@HiltViewModel
class UpdatesViewModel @Inject constructor(
    val updateHelper: UpdateHelper,
    private val downloadHelper: DownloadHelper,
    private val exodusRepository: ExodusRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    var updateAllEnqueued: Boolean = false

    private val _storageWarning = MutableSharedFlow<StorageRequirement>()
    val storageWarning = _storageWarning.asSharedFlow()

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

    fun cancelDownload(packageName: String) {
        viewModelScope.launch { downloadHelper.cancelDownload(packageName) }
    }

    fun cancelAll() {
        viewModelScope.launch { downloadHelper.cancelAll(true) }
    }
}
