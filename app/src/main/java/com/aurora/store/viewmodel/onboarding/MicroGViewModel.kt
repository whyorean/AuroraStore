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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

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
        private const val MICROG_DOWNLOAD_URL =
            "https://github.com/microg/GmsCore/releases/download"
        private const val MICROG_VERSION = "v0.3.11.250932"
        private const val GMS_VERSION_CODE = 250932022
        private const val COMPANION_VERSION_CODE = 84022622
    }

    init {
        AuroraApp.events.installerEvent.onEach {
            when (it) {
                is InstallerEvent.Installed -> confirmBundleInstall(it.packageName)
                else                        -> {}
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
        iconURL = "https://raw.githubusercontent.com/microg/GmsCore/refs/heads/master/play-services-core/src/main/res/mipmap-xxxhdpi/ic_app.png",
        developerName = "microG Team",
        fileList = listOf(
            PlayFile(
                url = "$MICROG_DOWNLOAD_URL/$MICROG_VERSION/$PACKAGE_NAME_GMS-$GMS_VERSION_CODE-hw.apk",
                name = "$PACKAGE_NAME_GMS-$GMS_VERSION_CODE-hw.apk",
                size = 92386474,
                sha256 = "2894a93544a8d7ca8f6ca96e7cc697647a7e0862165b6a02f8cd26822759b9cc"
            )
        )
    )

    private val microGCompanionApk = ExternalApk(
        packageName = PACKAGE_NAME_PLAY_STORE,
        versionCode = COMPANION_VERSION_CODE.toLong(),
        versionName = MICROG_VERSION,
        displayName = "microG Companion",
        iconURL = "https://raw.githubusercontent.com/microg/FakeStore/refs/heads/main/fake-store/src/main/res/mipmap-xxxhdpi/ic_app.png",
        developerName = "microG Team",
        fileList = listOf(
            PlayFile(
                url = "$MICROG_DOWNLOAD_URL/$MICROG_VERSION/$PACKAGE_NAME_PLAY_STORE-$COMPANION_VERSION_CODE-hw.apk",
                name = "$PACKAGE_NAME_PLAY_STORE-$COMPANION_VERSION_CODE-hw.apk",
                size = 4626291,
                sha256 = "b9623b8da8791c7e887efca941434b20e517c8a42ca4fda713625957edcc84eb"
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

    fun confirmBundleInstall(packageName: String) {
        if (PackageUtil.isMicroGBundleInstalled(context)) {
            uiState = uiState.copy(isInstalled = true, isDownloading = false)
        }
    }
}
