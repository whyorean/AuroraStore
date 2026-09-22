/*
 * SPDX-FileCopyrightText: 2021 Aurora OSS
 * SPDX-FileCopyrightText: 2022 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.extensions

import android.annotation.SuppressLint
import android.os.Build
import java.util.Locale

val isNAndAbove: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

val isNMR1AndAbove: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1

val isOAndAbove: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

val isPAndAbove: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

val isQAndAbove: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

val isRAndAbove: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

val isSAndAbove: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

val isTAndAbove: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

val isUAndAbove: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

val isVAndAbove: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM

val isMIUI: Boolean
    get() = !getSystemProperty("ro.miui.ui.version.name").isNullOrBlank()

val isHyperOS: Boolean
    get() = !getSystemProperty("ro.mi.os.version.name").isNullOrBlank() ||
        !getSystemProperty("ro.mi.os.version.code").isNullOrBlank()

val isGrapheneOS: Boolean
    get() = Build.FINGERPRINT.contains("GrapheneOS", ignoreCase = true) ||
        Build.HOST.contains("grapheneos", ignoreCase = true) ||
        getSystemProperty("ro.build.flavor")?.contains("grapheneos", ignoreCase = true) == true

val isHuawei: Boolean
    get() = Build.MANUFACTURER.lowercase(Locale.getDefault()).contains("huawei") ||
        Build.HARDWARE.lowercase(Locale.getDefault()).contains("kirin") ||
        Build.HARDWARE.lowercase(Locale.getDefault()).contains("hi3")

val isOneUI: Boolean
    get() = !getSystemProperty("ro.build.version.oneui").isNullOrBlank() ||
        Build.MANUFACTURER.equals("samsung", ignoreCase = true)

@get:SuppressLint("PrivateApi")
val isMiuiOptimizationDisabled: Boolean
    get() {
        return if ("0" == getSystemProperty("persist.sys.miui_optimization")) {
            true
        } else {
            try {
                Class.forName("android.miui.AppOpsUtils")
                    .getDeclaredMethod("isXOptMode")
                    .invoke(null) as Boolean
            } catch (_: java.lang.Exception) {
                false
            }
        }
    }

@SuppressLint("PrivateApi")
private fun getSystemProperty(key: String): String? = try {
    Class.forName("android.os.SystemProperties")
        .getDeclaredMethod("get", String::class.java)
        .invoke(null, key) as String
} catch (_: Exception) {
    null
}
