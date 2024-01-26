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

package com.aurora.store.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PackageInfoFlags
import android.content.pm.SharedLibraryInfo
import android.content.res.Configuration
import androidx.core.content.pm.PackageInfoCompat
import com.aurora.extensions.getInstallerPackageNameCompat
import com.aurora.extensions.isOAndAbove
import com.aurora.extensions.isTAndAbove


object PackageUtil {

    fun isInstalled(context: Context, packageName: String): Boolean {
        return try {
            getPackageInfo(context, packageName, PackageManager.GET_META_DATA)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun isInstalled(context: Context, packageName: String, versionCode: Int): Boolean {
        return try {
            val packageInfo = getPackageInfo(context, packageName)
            return PackageInfoCompat.getLongVersionCode(packageInfo) >= versionCode.toLong()
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun isSharedLibrary(context: Context, packageName: String): Boolean {
        return if (isOAndAbove()) {
            getAllSharedLibraries(context).any { it.name == packageName }
        } else {
            false
        }
    }

    fun isSharedLibraryInstalled(context: Context, packageName: String, versionCode: Int): Boolean {
        return if (isOAndAbove()) {
            val sharedLibraries = getAllSharedLibraries(context)
            sharedLibraries.any {
                it.name == packageName && it.version == versionCode
            }
        } else {
            false
        }
    }

    fun isUpdatable(context: Context, packageName: String, versionCode: Long): Boolean {
        return try {
            val packageInfo = getPackageInfo(context, packageName)
            return versionCode > PackageInfoCompat.getLongVersionCode(packageInfo)
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun getInstalledVersion(context: Context, packageName: String): String {
        return try {
            val packageInfo = getPackageInfo(context, packageName)
            "${packageInfo.versionName} (${PackageInfoCompat.getLongVersionCode(packageInfo)})"
        } catch (e: PackageManager.NameNotFoundException) {
            ""
        }
    }

    fun isTv(context: Context): Boolean {
        val uiMode = context.resources.configuration.uiMode
        return uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION
    }

    fun getLaunchIntent(context: Context, packageName: String?): Intent? {
        val intent = if (isTv(context)) {
            context.packageManager.getLeanbackLaunchIntentForPackage(packageName!!)
        } else {
            context.packageManager.getLaunchIntentForPackage(packageName!!)
        }

        return if (intent == null) {
            null
        } else {
            intent.addCategory(if (isTv(context)) Intent.CATEGORY_LEANBACK_LAUNCHER else Intent.CATEGORY_LAUNCHER)
            intent
        }
    }

    @Throws(Exception::class)
    fun getPackageInfo(context: Context, packageName: String, flags: Int = 0): PackageInfo {
        return if (isTAndAbove()) {
            context.packageManager.getPackageInfo(
                packageName,
                PackageInfoFlags.of(flags.toLong())
            )
        } else {
            context.packageManager.getPackageInfo(packageName, flags)
        }
    }

    private fun getAllSharedLibraries(context: Context, flags: Int = 0): List<SharedLibraryInfo> {
        return if (isTAndAbove()) {
            context.packageManager.getSharedLibraries(PackageInfoFlags.of(flags.toLong()))
        } else if (isOAndAbove()) {
            context.packageManager.getSharedLibraries(flags)
        } else {
            emptyList()
        }
    }

    fun getPackageInfoMap(context: Context): MutableMap<String, PackageInfo> {
        val packageInfoSet: MutableMap<String, PackageInfo> = mutableMapOf()
        val packageManager: PackageManager = context.packageManager
        val flags: Int = PackageManager.GET_META_DATA
        var packageInfoList: List<PackageInfo> = packageManager.getInstalledPackages(flags)

        val isGoogleFilterEnabled = Preferences.getBoolean(
            context,
            Preferences.PREFERENCE_FILTER_GOOGLE
        )

        val isAuroraOnlyUpdateEnabled = Preferences.getBoolean(
            context,
            Preferences.PREFERENCE_FILTER_AURORA_ONLY,
            false
        )

        val isFDroidFilterEnabled = Preferences.getBoolean(
            context,
            Preferences.PREFERENCE_FILTER_FDROID
        )

        val isExtendedUpdateEnabled = Preferences.getBoolean(
            context,
            Preferences.PREFERENCE_UPDATES_EXTENDED
        )

        packageInfoList = packageInfoList.filter {
            it.packageName != null && it.applicationInfo != null
        }

        /*Filter google apps*/
        if (isGoogleFilterEnabled) {
            packageInfoList = packageInfoList
                .filter {
                    !listOf(
                        "com.chrome.beta",
                        "com.chrome.canary",
                        "com.chrome.dev",
                        "com.android.chrome",
                        "com.niksoftware.snapseed",
                        "com.google.toontastic",
                    ).contains(it.packageName)
                }.filter {
                    it.packageName?.contains("com.google") == false
                }
        }

        /*Select only Aurora STore installed apps*/
        if (isAuroraOnlyUpdateEnabled) {
            packageInfoList = packageInfoList
                .filter {
                    val packageInstaller = packageManager.getInstallerPackageNameCompat(it.packageName)
                    listOf(
                        "com.aurora.store",
                        "com.aurora.store.nightly",
                        "com.aurora.services"
                    ).contains(packageInstaller)
                }
        }

        if (!isExtendedUpdateEnabled) {
            packageInfoList = packageInfoList.filter { it.applicationInfo.enabled }
        }

        /*Filter F-Droid apps*/
        if (isFDroidFilterEnabled) {
            packageInfoList = packageInfoList
                .filter {
                    val packageInstaller = packageManager.getInstallerPackageNameCompat(it.packageName)
                    !listOf(
                        "org.fdroid.fdroid",
                        "org.fdroid.fdroid.privileged"
                    ).contains(packageInstaller)
                }.filter {
                    !CertUtil.isFDroidApp(context, it.packageName)
                }
        }

        packageInfoList.forEach {
            packageInfoSet[it.packageName] = it
        }

        return packageInfoSet
    }


    fun getFilter(): IntentFilter {
        val filter = IntentFilter()
        filter.addDataScheme("package")
        filter.addAction(Intent.ACTION_PACKAGE_INSTALL)
        filter.addAction(Intent.ACTION_PACKAGE_ADDED)
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED)
        return filter
    }
}
