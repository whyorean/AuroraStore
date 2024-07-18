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
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.Base64
import android.util.Log
import com.aurora.extensions.generateX509Certificate
import com.aurora.extensions.getInstallerPackageNameCompat
import com.aurora.extensions.isPAndAbove
import com.aurora.store.util.PackageUtil.getPackageInfo
import java.security.MessageDigest
import java.security.cert.X509Certificate

object CertUtil {

    private val TAG = "CertUtil"

    fun isAuroraStoreApp(context: Context, packageName: String): Boolean {
        return context.packageManager.getInstallerPackageNameCompat(packageName) == context.packageName
    }

    fun isFDroidApp(context: Context, packageName: String): Boolean {
        return isInstalledByFDroid(context, packageName) || isSignedByFDroid(context, packageName)
    }

    fun isAppGalleryApp(context: Context, packageName: String): Boolean {
        val appGalleryPackageName = "com.huawei.appmarket"
        return context.packageManager.getInstallerPackageNameCompat(packageName) == appGalleryPackageName
    }

    fun getEncodedCertificateHashes(context: Context, packageName: String): List<String> {
        return try {
            val certificates = getX509Certificates(context, packageName)
            certificates.map {
                val messageDigest = MessageDigest.getInstance("SHA")
                messageDigest.update(it.encoded)
                Base64.encodeToString(
                    messageDigest.digest(),
                    Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
                )
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to get SHA256 certificate hash", exception)
            emptyList()
        }
    }

    private fun isSignedByFDroid(context: Context, packageName: String): Boolean {
        return try {
            getX509Certificates(context, packageName).any { cert ->
                cert.subjectDN.name.split(",").associate {
                    val (left, right) = it.split("=")
                    left to right
                }["O"] == "fdroid.org"
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to check signing cert for $packageName")
            false
        }
    }

    private fun isInstalledByFDroid(context: Context, packageName: String): Boolean {
        val fdroidPackages = listOf(
            "org.fdroid.basic", "org.fdroid.fdroid", "org.fdroid.fdroid.privileged"
        )
        return fdroidPackages.contains(
            context.packageManager.getInstallerPackageNameCompat(packageName)
        )
    }

    private fun getX509Certificates(context: Context, packageName: String): List<X509Certificate> {
        return try {
            val packageInfo = getPackageInfoWithSignature(context, packageName)
            if (isPAndAbove()) {
                if (packageInfo.signingInfo!!.hasMultipleSigners()) {
                    packageInfo.signingInfo!!.apkContentsSigners.map { it.generateX509Certificate() }
                } else {
                    packageInfo.signingInfo!!.signingCertificateHistory.map { it.generateX509Certificate() }
                }
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures!!.map { it.generateX509Certificate() }
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to get X509 certificates", exception)
            emptyList()
        }
    }

    private fun getPackageInfoWithSignature(context: Context, packageName: String): PackageInfo {
        return if (isPAndAbove()) {
            getPackageInfo(context, packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            @Suppress("DEPRECATION")
            getPackageInfo(context, packageName, PackageManager.GET_SIGNATURES)
        }
    }
}
