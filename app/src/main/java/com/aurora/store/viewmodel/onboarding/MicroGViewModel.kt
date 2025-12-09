/*
 * SPDX-FileCopyrightText: 2025 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.viewmodel.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.Constants.PACKAGE_NAME_GMS
import com.aurora.Constants.PACKAGE_NAME_PLAY_STORE
import com.aurora.gplayapi.data.models.PlayFile
import com.aurora.store.data.helper.DownloadHelper
import com.aurora.store.data.room.suite.ExternalApk
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MicroGViewModel @Inject constructor(
    private val downloadHelper: DownloadHelper
) : ViewModel() {

    companion object {
        private const val MICROG_DOWNLOAD_URL =
            "https://github.com/microg/GmsCore/releases/download"
        private const val MICROG_VERSION = "v0.3.6.244735"
        private const val GMS_VERSION_CODE = 244735012
        private const val COMPANION_VERSION_CODE = 84022612
    }

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
                size = 32509431,
                sha256 = "2f14df2974811b576bfafa6167a97e3b3032f2bd6e6ec3887a833fd2fa350dda"
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
                size = 3915551,
                sha256 = "6835b09016cef0fc3469b4a36b1720427ad3f81161cf20b188f0dadb5f8594e1"
            )
        )
    )

    fun downloadMicroG() {
        viewModelScope.launch(Dispatchers.IO) {
            downloadHelper.enqueueStandalone(microGServiceApk)
            downloadHelper.enqueueStandalone(microGCompanionApk)
        }
    }
}
