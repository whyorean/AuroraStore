/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.viewmodel.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.store.data.helper.UpdateHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(val updateHelper: UpdateHelper) : ViewModel() {

    private val _page = MutableStateFlow<OnboardingPage>(OnboardingPage.WELCOME)
    val currentPage: StateFlow<OnboardingPage> = _page

    fun setCurrentPage(page: OnboardingPage) {
        viewModelScope.launch {
            _page.emit(page)
        }
    }
}
