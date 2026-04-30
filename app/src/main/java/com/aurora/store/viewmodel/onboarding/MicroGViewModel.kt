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
import com.aurora.store.data.room.suite.ExternalApk
import com.aurora.store.util.PackageUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class MicroGUIState(
    var isDownloading: Boolean = false,
    var isInstalled: Boolean = false
)

@HiltViewModel
class MicroGViewModel @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val downloadHelper: DownloadHelper
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

    init {
        AuroraApp.events.installerEvent.onEach {
            when (it) {
                is InstallerEvent.Installed -> confirmBundleInstall()
                else -> {}
            }
        }.launchIn(AuroraApp.scope)
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

    fun downloadMicroG() {
        viewModelScope.launch(Dispatchers.IO) {
            if (microGCompanionApk.isInstalled(context)) {
                AuroraApp.events.send(InstallerEvent.Installed(PACKAGE_NAME_PLAY_STORE))
            } else {
                downloadHelper.enqueueStandalone(microGCompanionApk)
                uiState = uiState.copy(isDownloading = true)
            }

            if (microGServiceApk.isInstalled(context)) {
                AuroraApp.events.send(InstallerEvent.Installed(PACKAGE_NAME_GMS))
            } else {
                downloadHelper.enqueueStandalone(microGServiceApk)
                uiState = uiState.copy(isDownloading = true)
            }
        }
    }

    private fun confirmBundleInstall() {
        if (PackageUtil.isMicroGBundleInstalled(context)) {
            uiState = uiState.copy(isInstalled = true, isDownloading = false)
        }
    }
}
