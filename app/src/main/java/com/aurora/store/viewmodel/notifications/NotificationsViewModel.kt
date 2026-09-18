/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.viewmodel.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.store.AuroraApp
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.data.helper.DownloadHelper
import com.aurora.store.data.model.NotificationEntry
import com.aurora.store.data.room.notification.NotificationDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val downloadHelper: DownloadHelper,
    private val notificationDao: NotificationDao
) : ViewModel() {

    val entries: StateFlow<List<NotificationEntry>> = combine(
        downloadHelper.pendingInstalls,
        notificationDao.notifications()
    ) { pendingInstalls, remote ->
        val merged = pendingInstalls.map { NotificationEntry.PendingInstall(it) } +
            remote.map { NotificationEntry.Remote(it) }
        merged.sortedByDescending { it.timestamp }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val unreadCount: StateFlow<Int> = entries
        .map { list -> list.count { it.needsAttention } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), 0)

    private val _installing = MutableStateFlow<Set<String>>(emptySet())
    val installing = _installing.asStateFlow()

    init {
        AuroraApp.events.installerEvent.onEach { event ->
            _installing.update {
                when (event) {
                    is InstallerEvent.Installing -> it + event.packageName
                    // Pending user action deliberately doesn't clear: the session is still
                    // ours, and dropping it here flickered the indicator off and on again.
                    is InstallerEvent.Installed, is InstallerEvent.Failed ->
                        it - event.packageName

                    else -> it
                }
            }
        }.launchIn(viewModelScope)
    }

    fun install(packageName: String) {
        // Marked before handing off: staging a large session takes seconds, and a silent
        // install reports no progress at all, so events alone would show nothing.
        _installing.update { it + packageName }
        // Off the main thread: staging a session copies every APK, which blocks recomposition
        // (and risks an ANR) for as long as the app is large.
        viewModelScope.launch(Dispatchers.IO + NonCancellable) {
            if (!downloadHelper.installPending(packageName)) {
                _installing.update { it - packageName }
            }
        }
    }

    fun dismiss(entry: NotificationEntry) {
        viewModelScope.launch(Dispatchers.IO + NonCancellable) {
            when (entry) {
                is NotificationEntry.PendingInstall -> downloadHelper.dismissPendingInstall(
                    entry.download.packageName,
                    entry.download.versionCode
                )

                is NotificationEntry.Remote -> notificationDao.delete(entry.id)
            }
        }
    }

    fun markRead(entry: NotificationEntry) {
        if (entry !is NotificationEntry.Remote || entry.notification.isRead) return
        viewModelScope.launch { notificationDao.markRead(entry.id) }
    }

    private val NotificationEntry.needsAttention: Boolean
        get() = when (this) {
            is NotificationEntry.PendingInstall -> download.isAwaitingInstall
            is NotificationEntry.Remote -> !notification.isRead
        }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
