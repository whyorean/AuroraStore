/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.viewmodel.sheets

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.aurora.store.data.model.MinimalApp
import com.aurora.store.data.providers.BlacklistProvider
import com.aurora.store.data.work.ExportWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppMenuViewModel @Inject constructor(val blacklistProvider: BlacklistProvider) : ViewModel() {

    fun copyInstalledApp(context: Context, app: MinimalApp, uri: Uri) {
        ExportWorker.exportInstalledApp(context, app, uri)
    }
}
