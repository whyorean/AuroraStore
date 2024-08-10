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

package com.aurora.store.data.providers

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.pm.PackageInfoCompat
import com.aurora.store.util.PackageUtil.getPackageInfo

class NativeGsfVersionProvider(context: Context, isExport: Boolean = false) {
    private val GOOGLE_SERVICES_PACKAGE_ID = "com.google.android.gms"
    private val GOOGLE_VENDING_PACKAGE_ID = "com.android.vending"

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
