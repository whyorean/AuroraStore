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
import android.content.res.Configuration
import android.os.Build


object PackageUtil {

    fun isInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun isInstalled(context: Context, packageName: String, versionCode: Int): Boolean {
        return try {
            val packageInfo = getPackageInfo(context, packageName)
            if (packageInfo != null) {
                return packageInfo.versionCode >= versionCode
            }
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun isUpdatable(context: Context, packageName: String, versionCode: Long): Boolean {
        return try {
            val packageInfo = getPackageInfo(context, packageName)
            if (packageInfo != null) {
                return versionCode > packageInfo.versionCode
            }
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun getInstalledVersion(context: Context, packageName: String): String {
        return try {
            val packageInfo = getPackageInfo(context, packageName)
            if (packageInfo != null) {
                "${packageInfo.versionName}.${packageInfo.versionCode}"
            } else {
                ""
            }
        } catch (e: PackageManager.NameNotFoundException) {
            ""
        }
    }

    fun isTv(context: Context): Boolean {
        val uiMode = context.resources.configuration.uiMode
        return uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION
    }

    fun getLaunchIntent(context: Context, packageName: String?): Intent? {
        val isTv = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && isTv(context)
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (isTv) {
                context.packageManager.getLeanbackLaunchIntentForPackage(packageName!!)
            } else {
                context.packageManager.getLaunchIntentForPackage(packageName!!)
            }
        } else {
            context.packageManager.getLaunchIntentForPackage(packageName!!)
        }

        return if (intent == null) {
            null
        } else {
            intent.addCategory(if (isTv) Intent.CATEGORY_LEANBACK_LAUNCHER else Intent.CATEGORY_LAUNCHER)
            intent
        }
    }

    @Throws(Exception::class)
    fun getPackageInfo(context: Context, packageName: String?): PackageInfo? {
        return context.packageManager.getPackageInfo(packageName!!, 0)
    }

    fun getAllPackages(context: Context): List<PackageInfo> {
        val packageInfoSet: MutableList<PackageInfo> = mutableListOf()
        val packageManager: PackageManager = context.packageManager
        val flags: Int = getAllFlags()
        val packageInfoList: List<PackageInfo> = packageManager.getInstalledPackages(flags)
        for (packageInfo in packageInfoList) {
            if (packageInfo.packageName != null && packageInfo.applicationInfo != null) {
                packageInfoSet.add(packageInfo)
            }
        }
        return packageInfoSet
    }

    fun getPackageInfoMap(context: Context): MutableMap<String, PackageInfo> {
        val packageInfoSet: MutableMap<String, PackageInfo> = mutableMapOf()
        val packageManager: PackageManager = context.packageManager
        val flags: Int = PackageManager.GET_META_DATA
        var packageInfoList: List<PackageInfo> = packageManager.getInstalledPackages(flags)

        val isFdroidFilterEnabled = Preferences.getBoolean(
            context,
            Preferences.PREFERENCE_FILTER_FDROID
        )

        packageInfoList = packageInfoList
            .filter {
                it.packageName != null && it.applicationInfo != null && it.applicationInfo.enabled
            }

        if (isFdroidFilterEnabled) {
            packageInfoList = packageInfoList
                .filter {
                    val packageInstaller = packageManager.getInstallerPackageName(it.packageName)
                    packageInstaller != "org.fdroid.fdroid.privileged"
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

    private fun getAllFlags(): Int {
        var flags = (PackageManager.GET_META_DATA
                or PackageManager.GET_ACTIVITIES
                or PackageManager.GET_SERVICES
                or PackageManager.GET_PROVIDERS
                or PackageManager.GET_RECEIVERS)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            flags = flags or PackageManager.GET_DISABLED_COMPONENTS
            flags = flags or PackageManager.GET_UNINSTALLED_PACKAGES
        } else {
            flags = flags or PackageManager.MATCH_DISABLED_COMPONENTS
            flags = flags or PackageManager.MATCH_UNINSTALLED_PACKAGES
        }
        return flags
    }
}