/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store

import androidx.lifecycle.ViewModel
import com.aurora.store.data.helper.UpdateHelper
import com.aurora.store.data.providers.NetworkProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    val networkProvider: NetworkProvider,
    val updateHelper: UpdateHelper
) : ViewModel()
