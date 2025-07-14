/*
 * SPDX-FileCopyrightText: 2025 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.viewmodel.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.Constants.PACKAGE_NAME_GMS
import com.aurora.Constants.PACKAGE_NAME_PLAY_STORE
import com.aurora.gplayapi.data.models.PlayFile
import com.aurora.store.AuroraApp
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.data.helper.DownloadHelper
import com.aurora.store.data.room.suite.ExternalApk
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MicroGViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadHelper: DownloadHelper
) : ViewModel() {

    companion object {
        const val MICROG_DOWNLOAD_URL = "https://github.com/microg/GmsCore/releases/download"
        const val MICROG_VERSION = "v0.3.6.244735"
        const val GMS_VERSION_CODE = 244735012
        const val COMPANION_VERSION_CODE = 84022612
    }

    private val _packageNames = MutableSharedFlow<List<String>>()
    val packageNames = _packageNames.asSharedFlow()

    private val _checked = MutableStateFlow(false)
    val checked: StateFlow<Boolean> = _checked

    val download = combine(packageNames, downloadHelper.downloadsList) { apps, list ->
        list.find { d -> apps.any { it == d.packageName } }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val microGServiceApk = ExternalApk(
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
                size = 32509431,
                sha256 = "2f14df2974811b576bfafa6167a97e3b3032f2bd6e6ec3887a833fd2fa350dda"
            )
        )
    )

    val microGCompanionApk = ExternalApk(
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
                size = 3915551,
                sha256 = "6835b09016cef0fc3469b4a36b1720427ad3f81161cf20b188f0dadb5f8594e1"
            )
        )
    )

    fun markAgreement(checked: Boolean) {
        viewModelScope.launch {
            _checked.emit(checked)
        }
    }

    fun downloadMicroG() {
        viewModelScope.launch {
            try {
                _packageNames.emit(listOf(PACKAGE_NAME_GMS))

                if (microGServiceApk.isInstalled(context)) {
                    AuroraApp.events.send(InstallerEvent.Installed(PACKAGE_NAME_GMS))
                } else {
                    downloadHelper.enqueueStandalone(microGServiceApk)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun downloadCompanion() {
        viewModelScope.launch {
            try {
                _packageNames.emit(listOf(PACKAGE_NAME_PLAY_STORE))

                if (microGCompanionApk.isInstalled(context)) {
                    AuroraApp.events.send(InstallerEvent.Installed(PACKAGE_NAME_PLAY_STORE))
                } else {
                    downloadHelper.enqueueStandalone(microGCompanionApk)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
