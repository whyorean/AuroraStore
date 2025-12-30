/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.viewmodel.preferences

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.extensions.TAG
import com.aurora.extensions.isMIUI
import com.aurora.extensions.isMiuiOptimizationDisabled
import com.aurora.extensions.observeAsStateFlow
import com.aurora.store.R
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.data.model.Installer
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_INSTALLER_ID
import com.aurora.store.util.save
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import rikka.sui.Sui
import javax.inject.Inject

@HiltViewModel
class InstallerViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel(), Shizuku.OnBinderReceivedListener, Shizuku.OnBinderDeadListener,
    Shizuku.OnRequestPermissionResultListener {

    private val sharedPreferences = Preferences.getPrefs(context)
    private var isShizukuAlive = Sui.isSui()

    private val _error = MutableSharedFlow<String>()
    val error = _error.asSharedFlow()

    private var _installerId: Int
        get() = sharedPreferences.getInt(PREFERENCE_INSTALLER_ID, 0)
        set(value) = sharedPreferences.edit { putInt(PREFERENCE_INSTALLER_ID, value) }

    val currentInstaller = sharedPreferences.observeAsStateFlow(
        key = PREFERENCE_INSTALLER_ID,
        scope = viewModelScope,
        initial = AppInstaller.getCurrentInstaller(context).ordinal,
        valueProvider = { _installerId }
    )

    init {
        Shizuku.addBinderReceivedListenerSticky(this)
        Shizuku.addBinderDeadListener(this)
        Shizuku.addRequestPermissionResultListener(this)
    }

    override fun onBinderReceived() {
        isShizukuAlive = true
    }

    override fun onBinderDead() {
        isShizukuAlive = false
    }

    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            save(Installer.SHIZUKU)
        } else {
            viewModelScope.launch {
                Log.e(TAG, "Permission denied for shizuku")
                _error.emit(context.getString(R.string.permissions_denied))
            }
        }
    }

    fun save(installer: Installer) {
        viewModelScope.launch {
            // Error handling for different installers
            when (installer) {
                Installer.SESSION -> {
                    if (isMIUI && !isMiuiOptimizationDisabled) {
                        Log.e(TAG, "Trying to set session installer with MIUI optimizations")
                        _error.emit(context.getString(R.string.device_miui_description))
                        return@launch
                    }
                }

                Installer.ROOT -> {
                    if (!AppInstaller.hasRootAccess()) {
                        Log.e(TAG, "Trying to set root installer without root access")
                        _error.emit(context.getString(R.string.installer_root_unavailable))
                        return@launch
                    }
                }

                Installer.SERVICE -> {
                    if (!AppInstaller.hasAuroraService(context)) {
                        Log.e(TAG, "Trying to set service installer without companion app")
                        _error.emit(context.getString(R.string.installer_service_unavailable))
                        return@launch
                    }
                }

                Installer.SHIZUKU -> {
                    if (AppInstaller.hasShizukuOrSui(context) && isShizukuAlive) {
                        if (!AppInstaller.hasShizukuPerm()) {
                            Log.i(TAG, "Requesting permission for shizuku")
                            Shizuku.requestPermission(9000)
                            return@launch
                        }
                    } else {
                        Log.e(TAG, "Trying to set shizuku installer without appropriate setup")
                        _error.emit(context.getString(R.string.installer_shizuku_unavailable))
                        return@launch
                    }
                }

                Installer.AM -> {
                    if (!AppInstaller.hasAppManager(context)) {
                        Log.e(TAG, "Trying to set AM installer without companion app")
                        _error.emit(context.getString(R.string.installer_am_unavailable))
                        return@launch
                    }
                }

                else -> Log.i(TAG, "Trying to set ${installer.name} installer without any checks")
            }

            context.save(PREFERENCE_INSTALLER_ID, installer.ordinal)
        }
    }
}
