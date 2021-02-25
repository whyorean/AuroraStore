/*
 * Aurora Store
 *  Copyright (C) 2021, Rahul Kumar Patel <whyorean@gmail.com>
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
import java.util.*


fun isLAndAbove(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
}

fun isMAndAbove(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
}

fun isNAndAbove(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
}

fun isOAndAbove(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
}

fun isPAndAbove(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
}

fun isQAndAbove(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
}

fun isRAndAbove(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
}

fun isMIUI(): Boolean {
    return getSystemProperty("ro.miui.ui.version.name").isNotEmpty()
}

fun isHuawei(): Boolean {
    return Build.MANUFACTURER.toLowerCase(Locale.getDefault()).contains("huawei")
            || Build.HARDWARE.toLowerCase(Locale.getDefault()).contains("kirin")
            || Build.HARDWARE.toLowerCase(Locale.getDefault()).contains("hi3")
}

fun getMIUIVersion(): String {
    val version = getSystemProperty("ro.miui.ui.version.name")
    val versionCode = getSystemProperty("ro.miui.ui.version.code")
    return if (version.isNotEmpty() && versionCode.isNotEmpty())
        "$version.$versionCode"
    else
        "unknown"
}

@SuppressLint("PrivateApi")
fun isMiuiOptimizationDisabled(): Boolean {
    if ("0" == getSystemProperty("persist.sys.miui_optimization")) {
        return true
    } else try {
        return Class.forName("android.miui.AppOpsUtils")
            .getDeclaredMethod("isXOptMode")
            .invoke(null) as Boolean
    } catch (e: java.lang.Exception) {
        return false
    }
}

@SuppressLint("PrivateApi")
fun getSystemProperty(key: String): String {
    return try {
        Class.forName("android.os.SystemProperties")
            .getDeclaredMethod("get", String::class.java)
            .invoke(null, key) as String
    } catch (e: Exception) {
        ""
    }
}