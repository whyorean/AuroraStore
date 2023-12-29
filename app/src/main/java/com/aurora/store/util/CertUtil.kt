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
import com.aurora.extensions.isPAndAbove
import com.aurora.store.util.PackageUtil.getPackageInfo
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.util.Locale

object CertUtil {

    private val TAG = "CertUtil"
    private val fdroidSubjects = listOf("FDROID", "GUARDIANPROJECT.INFO")

    fun isFDroidApp(context: Context, packageName: String): Boolean {
        return getX509Certificates(
            context,
            packageName
        ).any { it.subjectDN.name.uppercase(Locale.getDefault()) in fdroidSubjects }
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

    private fun getX509Certificates(context: Context, packageName: String): List<X509Certificate> {
        return try {
            val packageInfo = getPackageInfoWithSignature(context, packageName)
            if (isPAndAbove()) {
                if (packageInfo.signingInfo.hasMultipleSigners()) {
                    packageInfo.signingInfo.apkContentsSigners.map { it.generateX509Certificate() }
                } else {
                    packageInfo.signingInfo.signingCertificateHistory.map { it.generateX509Certificate() }
                }
            } else {
                packageInfo.signatures.map { it.generateX509Certificate() }
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
            getPackageInfo(context, packageName, PackageManager.GET_SIGNATURES)
        }
    }
}
