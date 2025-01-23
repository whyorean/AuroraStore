/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.viewmodel.sheets

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.aurora.store.data.helper.DownloadHelper
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.data.room.download.Download
import com.aurora.store.data.work.ExportWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DownloadMenuViewModel @Inject constructor(
    val downloadHelper: DownloadHelper,
    val appInstaller: AppInstaller
) : ViewModel() {

    fun copyDownloadedApp(context: Context, download: Download, uri: Uri) {
        ExportWorker.exportDownloadedApp(context, download, uri)
    }
}
