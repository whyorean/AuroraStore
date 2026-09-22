/*
 * SPDX-FileCopyrightText: 2021 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.providers

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.pm.PackageInfoCompat
import com.aurora.store.util.PackageUtil.getPackageInfo

class NativeGsfVersionProvider(context: Context, isExport: Boolean = false) {

    companion object {
        private const val GOOGLE_SERVICES_PACKAGE_ID = "com.google.android.gms"
        private const val GOOGLE_VENDING_PACKAGE_ID = "com.android.vending"
    }

    // Preferred defaults, not any specific reason they just work fine.
    var gsfVersionCode = 203019037L
    var vendingVersionCode = 82151710L
    var vendingVersionString = "21.5.17-21 [0] [PR] 326734551"

    init {
        try {
            if (isExport) {
                getPackageInfo(context, GOOGLE_SERVICES_PACKAGE_ID).let {
                    gsfVersionCode = PackageInfoCompat.getLongVersionCode(it)
                }

                getPackageInfo(context, GOOGLE_VENDING_PACKAGE_ID).let {
                    vendingVersionCode = PackageInfoCompat.getLongVersionCode(it)
                    vendingVersionString = it.versionName ?: vendingVersionString
                }
            }
        } catch (_: PackageManager.NameNotFoundException) {
        }
    }
}
