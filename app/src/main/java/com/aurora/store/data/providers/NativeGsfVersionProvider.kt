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

class NativeGsfVersionProvider(context: Context) {
    private var gsfVersionCode = 0
    private var vendingVersionCode = 0
    private var vendingVersionString = ""

    init {
        try {
            gsfVersionCode =
                context.packageManager.getPackageInfo(GOOGLE_SERVICES_PACKAGE_ID, 0).versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            // com.google.android.gms not found
        }
        try {
            val packageInfo = context.packageManager.getPackageInfo(GOOGLE_VENDING_PACKAGE_ID, 0)
            vendingVersionCode = packageInfo.versionCode
            vendingVersionString = packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            // com.android.vending not found
        }
    }

    init {
        try {
            gsfVersionCode =
                context.packageManager.getPackageInfo(GOOGLE_SERVICES_PACKAGE_ID, 0).versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            // com.google.android.gms not found
        }
        try {
            val packageInfo = context.packageManager.getPackageInfo(GOOGLE_VENDING_PACKAGE_ID, 0)
            vendingVersionCode = packageInfo.versionCode
            vendingVersionString = packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            // com.android.vending not found
        }
    }

    fun getGsfVersionCode(defaultIfNotFound: Boolean): Int {
        return if (defaultIfNotFound && gsfVersionCode < GOOGLE_SERVICES_VERSION_CODE)
            GOOGLE_SERVICES_VERSION_CODE
        else
            gsfVersionCode
    }

    fun getVendingVersionCode(defaultIfNotFound: Boolean): Int {
        return if (defaultIfNotFound && vendingVersionCode < GOOGLE_VENDING_VERSION_CODE)
            GOOGLE_VENDING_VERSION_CODE
        else
            vendingVersionCode
    }

    fun getVendingVersionString(defaultIfNotFound: Boolean): String {
        return if (defaultIfNotFound && vendingVersionCode < GOOGLE_VENDING_VERSION_CODE)
            GOOGLE_VENDING_VERSION_STRING
        else
            vendingVersionString
    }

    companion object {
        private const val GOOGLE_SERVICES_PACKAGE_ID = "com.google.android.gms"
        private const val GOOGLE_VENDING_PACKAGE_ID = "com.android.vending"
        private const val GOOGLE_SERVICES_VERSION_CODE = 203019037
        private const val GOOGLE_VENDING_VERSION_CODE = 82151710
        private const val GOOGLE_VENDING_VERSION_STRING = "21.5.17-21 [0] [PR] 326734551"
    }
}