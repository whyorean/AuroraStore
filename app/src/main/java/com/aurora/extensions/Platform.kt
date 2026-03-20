/*
 * Aurora Store
 *  Copyright (C) 2021, Rahul Kumar Patel <whyorean@gmail.com>
 *  Copyright (C) 2022, The Calyx Institute
 *
 *  Aurora Store is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Aurora Store is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.aurora.extensions

import android.annotation.SuppressLint
import android.os.Build
import java.util.Locale

val isNAndAbove: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

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

val isHuawei: Boolean
    get() = Build.MANUFACTURER.lowercase(Locale.getDefault()).contains("huawei") ||
        Build.HARDWARE.lowercase(Locale.getDefault()).contains("kirin") ||
        Build.HARDWARE.lowercase(Locale.getDefault()).contains("hi3")

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
