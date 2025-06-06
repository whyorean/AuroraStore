/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.viewmodel.games

import androidx.lifecycle.ViewModel
import com.aurora.store.data.providers.AuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class GamesContainerViewModel @Inject constructor(val authProvider: AuthProvider) : ViewModel()
