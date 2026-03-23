/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.viewmodel.downloads

import androidx.lifecycle.ViewModel
import com.aurora.store.data.helper.DownloadHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DownloadViewModel @Inject constructor(val downloadHelper: DownloadHelper) : ViewModel()
