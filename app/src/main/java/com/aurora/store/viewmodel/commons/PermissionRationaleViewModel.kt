/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.viewmodel.commons

import android.content.Context
import androidx.lifecycle.ViewModel
import com.aurora.store.data.providers.PermissionProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class PermissionRationaleViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _permissions = MutableStateFlow(PermissionProvider.getAllKnownPermissions(context))
    val permissions = _permissions.asStateFlow()

    fun refreshPermissionsList() {
        _permissions.value = _permissions.value.map { permission ->
            permission.copy(
                isGranted = PermissionProvider.isGranted(
                    context,
                    permission.type
                )
            )
        }
    }
}
