/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.viewmodel.preferences

import androidx.lifecycle.ViewModel
import com.aurora.store.data.helper.UpdateHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class UpdatesRestrictionsViewModel @Inject constructor(val updateHelper: UpdateHelper) : ViewModel()
