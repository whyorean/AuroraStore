/*
 * SPDX-FileCopyrightText: 2025 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.viewmodel.onboarding

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.Constants.PACKAGE_NAME_GMS
import com.aurora.Constants.PACKAGE_NAME_PLAY_STORE
import com.aurora.gplayapi.data.models.PlayFile
import com.aurora.store.AuroraApp
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.data.helper.DownloadHelper
import com.aurora.store.data.model.DownloadStatus
import com.aurora.store.data.model.NetworkStatus
import com.aurora.store.data.providers.NetworkProvider
import com.aurora.store.data.room.download.Download
import com.aurora.store.data.room.suite.ExternalApk
import com.aurora.store.util.PackageUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

enum class MicroGItemState { PENDING, DOWNLOADING, INSTALLING, INSTALLED, FAILED }

data class MicroGItem(
    val packageName: String,
    val displayName: String,
    val iconURL: String,
    val totalBytes: Long,
    val state: MicroGItemState,
    val progress: Int = 0,
    val speed: Long = 0L,
    val timeRemaining: Long = -1L
)

data class MicroGUIState(
    val items: List<MicroGItem> = emptyList(),
    val isOnline: Boolean = true
) {
    val isInstalled: Boolean
        get() = items.isNotEmpty() && items.all { it.state == MicroGItemState.INSTALLED }
    val isInProgress: Boolean
        get() = items.any {
            it.state == MicroGItemState.DOWNLOADING || it.state == MicroGItemState.INSTALLING
        }
    val hasFailed: Boolean
        get() = !isInstalled && items.any { it.state == MicroGItemState.FAILED }
}

@HiltViewModel
class MicroGViewModel @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val downloadHelper: DownloadHelper,
    networkProvider: NetworkProvider
) : ViewModel() {

    companion object {
        private const val BASE_URL =
            "https://github.com/microg/GmsCore/releases/download"
        private const val MICROG_VERSION = "v0.3.15.250932"
        private const val GMS_VERSION_CODE = 250932030
        private const val COMPANION_VERSION_CODE = 84022630

        private const val MICROG_DOWNLOAD_URL =
            "$BASE_URL/$MICROG_VERSION/$PACKAGE_NAME_GMS-$GMS_VERSION_CODE-hw.apk"
        private const val FAKE_STORE_DOWNLOAD_URL =
            "$BASE_URL/$MICROG_VERSION/$PACKAGE_NAME_PLAY_STORE-$COMPANION_VERSION_CODE-hw.apk"

        private const val ICON_BASE_URL = "https://raw.githubusercontent.com/microg"
        private const val ICON_FILE_PATH = "src/main/res/mipmap-xxxhdpi/ic_app.png"
    }

    var uiState: MicroGUIState by mutableStateOf(MicroGUIState())
        private set

    private val microGServiceApk = ExternalApk(
        packageName = PACKAGE_NAME_GMS,
        versionCode = GMS_VERSION_CODE.toLong(),
        versionName = MICROG_VERSION,
        displayName = "microG Services",
        iconURL = "$ICON_BASE_URL/GmsCore/refs/heads/master/play-services-core/$ICON_FILE_PATH",
        developerName = "microG Team",
        fileList = listOf(
            PlayFile(
                url = MICROG_DOWNLOAD_URL,
                name = "$PACKAGE_NAME_GMS-$GMS_VERSION_CODE-hw.apk",
                size = 92834436,
                sha256 = "896de0917313504cd8406b725b4cb420bfea26e1a50f7dd37f9175c0bfbab2ac"
            )
        )
    )

    private val microGCompanionApk = ExternalApk(
        packageName = PACKAGE_NAME_PLAY_STORE,
        versionCode = COMPANION_VERSION_CODE.toLong(),
        versionName = MICROG_VERSION,
        displayName = "microG Companion",
        iconURL = "$ICON_BASE_URL/FakeStore/refs/heads/main/fake-store/$ICON_FILE_PATH",
        developerName = "microG Team",
        fileList = listOf(
            PlayFile(
                url = FAKE_STORE_DOWNLOAD_URL,
                name = "$PACKAGE_NAME_PLAY_STORE-$COMPANION_VERSION_CODE-hw.apk",
                size = 4641800,
                sha256 = "a215e44bd89a4e5078fd7babf7baa7b47b69ac27fca13e9c0abfedc33cb087d7"
            )
        )
    )

    private val bundle = listOf(microGServiceApk, microGCompanionApk)

    init {
        AuroraApp.events.installerEvent.onEach {
            if (it is InstallerEvent.Installed) refresh()
        }.launchIn(AuroraApp.scope)

        combine(
            downloadHelper.downloadsList,
            networkProvider.status.onStart { emit(NetworkStatus.AVAILABLE) }
        ) { downloads, network ->
            val byPackage = downloads.associateBy { it.packageName }
            MicroGUIState(
                items = bundle.map { apk -> buildItem(apk, byPackage[apk.packageName]) },
                isOnline = network == NetworkStatus.AVAILABLE
            )
        }.onEach { uiState = it }.launchIn(viewModelScope)
    }

    fun downloadMicroG() {
        viewModelScope.launch(Dispatchers.IO) {
            bundle.forEach { enqueueIfNeeded(it) }
        }
    }

    fun retryDownload() {
        viewModelScope.launch(Dispatchers.IO) {
            bundle.forEach { apk ->
                downloadHelper.getDownload(apk.packageName)?.takeIf { it.isFinished }?.let {
                    downloadHelper.clearDownload(apk.packageName, apk.versionCode)
                }
                enqueueIfNeeded(apk)
            }
        }
    }

    private suspend fun enqueueIfNeeded(apk: ExternalApk) {
        if (apk.isInstalled(context)) {
            AuroraApp.events.send(InstallerEvent.Installed(apk.packageName))
        } else {
            downloadHelper.enqueueStandalone(apk)
        }
    }

    private fun refresh() {
        uiState = uiState.copy(
            items = uiState.items.map { item ->
                if (item.state != MicroGItemState.INSTALLED &&
                    PackageUtil.isInstalled(context, item.packageName)
                ) {
                    item.copy(state = MicroGItemState.INSTALLED)
                } else {
                    item
                }
            }
        )
    }

    private fun buildItem(apk: ExternalApk, download: Download?): MicroGItem {
        val state = when {
            apk.isInstalled(context) -> MicroGItemState.INSTALLED
            download == null -> MicroGItemState.PENDING
            download.status == DownloadStatus.FAILED -> MicroGItemState.FAILED
            download.status == DownloadStatus.CANCELLED -> MicroGItemState.PENDING
            download.status == DownloadStatus.COMPLETED -> MicroGItemState.INSTALLING
            else -> MicroGItemState.DOWNLOADING
        }

        return MicroGItem(
            packageName = apk.packageName,
            displayName = apk.displayName,
            iconURL = apk.iconURL,
            totalBytes = apk.fileList.sumOf { it.size },
            state = state,
            progress = download?.progress ?: 0,
            speed = download?.speed ?: 0L,
            timeRemaining = download?.timeRemaining ?: -1L
        )
    }
}
