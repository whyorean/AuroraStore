/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.viewmodel.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.data.models.PlayFile
import com.aurora.store.data.helper.DownloadHelper
import com.aurora.store.data.room.suite.ExternalApk
import com.aurora.store.util.PackageUtil
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

    private val _packageNames = MutableSharedFlow<List<String>>()
    val packageNames = _packageNames.asSharedFlow()

    private val _checked = MutableStateFlow(false)
    val checked: StateFlow<Boolean> = _checked

    val download = combine(packageNames, downloadHelper.downloadsList) { apps, list ->
        list.find { d -> apps.any { it == d.packageName } }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val externalApks = listOf<ExternalApk>(
        ExternalApk(
            packageName = "com.google.android.gms",
            versionCode = 244735012,
            versionName = "v0.3.6.244735",
            displayName = "microG Services",
            iconURL = "https://raw.githubusercontent.com/microg/GmsCore/refs/heads/master/play-services-core/src/main/res/mipmap-xxxhdpi/ic_app.png",
            developerName = "microG Team",
            fileList = listOf(
                PlayFile(
                    url = "https://github.com/microg/GmsCore/releases/download/v0.3.6.244735/com.google.android.gms-244735012-hw.apk",
                    name = "com.google.android.gms-244735012-hw.apk",
                    size = 32509431,
                    sha256 = "2f14df2974811b576bfafa6167a97e3b3032f2bd6e6ec3887a833fd2fa350dda"
                )
            )
        ),
        ExternalApk(
            packageName = "com.android.vending",
            versionCode = 84022612,
            versionName = "v0.3.6.244735",
            displayName = "microG Companion",
            iconURL = "https://raw.githubusercontent.com/microg/FakeStore/refs/heads/main/fake-store/src/main/res/mipmap-xxxhdpi/ic_app.png",
            developerName = "microG Team",
            fileList = listOf(
                PlayFile(
                    url = "https://github.com/microg/GmsCore/releases/download/v0.3.6.244735/com.android.vending-84022612-hw.apk",
                    name = "com.android.vending-84022612-hw.apk",
                    size = 3915551,
                    sha256 = "6835b09016cef0fc3469b4a36b1720427ad3f81161cf20b188f0dadb5f8594e1"
                )
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
                _packageNames.emit(
                    listOf(
                        PackageUtil.PACKAGE_NAME_GMS,
                        PackageUtil.PACKAGE_NAME_VENDING
                    )
                )

                externalApks.forEach {
                    // Enqueue download only if not already installed
                    if (!it.isInstalled(context)) {
                        downloadHelper.enqueueStandalone(it)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
