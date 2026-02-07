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
import com.aurora.Constants.PACKAGE_NAME_APP_GALLERY
import com.aurora.Constants.PACKAGE_NAME_GMS
import com.aurora.extensions.TAG
import com.aurora.extensions.generateX509Certificate
import com.aurora.extensions.getUpdateOwnerPackageNameCompat
import com.aurora.extensions.isPAndAbove
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.data.model.Algorithm
import com.aurora.store.util.PackageUtil.getPackageInfo
import java.security.MessageDigest
import java.security.cert.X509Certificate
import javax.security.auth.x500.X500Principal

object CertUtil {

    const val GOOGLE_ACCOUNT_TYPE = "com.google"
    const val GOOGLE_PLAY_AUTH_TOKEN_TYPE = "oauth2:https://www.googleapis.com/auth/googleplay"
    const val GOOGLE_PLAY_CERT =
        "MIIEQzCCAyugAwIBAgIJAMLgh0ZkSjCNMA0GCSqGSIb3DQEBBAUAMHQxCzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpDYWxpZm9ybmlhMRYwFAYDVQQHEw1Nb3VudGFpbiBWaWV3MRQwEgYDVQQKEwtHb29nbGUgSW5jLjEQMA4GA1UECxMHQW5kcm9pZDEQMA4GA1UEAxMHQW5kcm9pZDAeFw0wODA4MjEyMzEzMzRaFw0zNjAxMDcyMzEzMzRaMHQxCzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpDYWxpZm9ybmlhMRYwFAYDVQQHEw1Nb3VudGFpbiBWaWV3MRQwEgYDVQQKEwtHb29nbGUgSW5jLjEQMA4GA1UECxMHQW5kcm9pZDEQMA4GA1UEAxMHQW5kcm9pZDCCASAwDQYJKoZIhvcNAQEBBQADggENADCCAQgCggEBAKtWLgDYO6IIrgqWbxJOKdoR8qtW0I9Y4sypEwPpt1TTcvZApxsdyxMJZ2JORland2qSGT2y5b+3JKkedxiLDmpHpDsz2WCbdxgxRczfey5YZnTJ4VZbH0xqWVW/8lGmPav5xVwnIiJS6HXk+BVKZF+JcWjAsb/GEuq/eFdpuzSqeYTcfi6idkyugwfYwXFU1+5fZKUaRKYCwkkFQVfcAs1fXA5V+++FGfvjJ/CxURaSxaBvGdGDhfXE28LWuT9ozCl5xw4Yq5OGazvV24mZVSoOO0yZ31j7kYvtwYK6NeADwbSxDdJEqO4k//0zOHKrUiGYXtqw/A0LFFtqoZKFjnkCAQOjgdkwgdYwHQYDVR0OBBYEFMd9jMIhF1Ylmn/Tgt9r45jk14alMIGmBgNVHSMEgZ4wgZuAFMd9jMIhF1Ylmn/Tgt9r45jk14aloXikdjB0MQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNTW91bnRhaW4gVmlldzEUMBIGA1UEChMLR29vZ2xlIEluYy4xEDAOBgNVBAsTB0FuZHJvaWQxEDAOBgNVBAMTB0FuZHJvaWSCCQDC4IdGZEowjTAMBgNVHRMEBTADAQH/MA0GCSqGSIb3DQEBBAUAA4IBAQBt0lLO74UwLDYKqs6Tm8/yzKkEu116FmH4rkaymUIE0P9KaMftGlMexFlaYjzmB2OxZyl6euNXEsQH8gjwyxCUKRJNexBiGcCEyj6z+a1fuHHvkiaai+KL8W1EyNmgjmyy8AW7P+LLlkR+ho5zEHatRbM/YAnqGcFh5iZBqpknHf1SKMXFh4dd239FJ1jWYfbMDMy3NS5CTMQ2XFI1MvcyUTdZPErjQfTbQe3aDQsQcafEQPD+nqActifKZ0Np0IS9L9kR/wbNvyz6ENwPiTrjV2KRkEjH78ZMcUQXg0L3BYHJ3lc69Vs5Ddf9uUGGMYldX3WfMBEmh/9iFBDAaTCK"

    private val fdroidPackages = listOf(
        "org.fdroid.basic",
        "org.fdroid.fdroid",
        "org.fdroid.fdroid.privileged",
        "com.looker.droidify",
        "com.machiav3lli.fdroid"
    )

    fun isFDroidApp(context: Context, packageName: String?): Boolean =
        isInstalledByFDroid(context, packageName) || isSignedByFDroid(context, packageName)

    fun isAppGalleryApp(context: Context, packageName: String): Boolean =
        context.packageManager.getUpdateOwnerPackageNameCompat(packageName) ==
            PACKAGE_NAME_APP_GALLERY

    fun isAuroraStoreApp(context: Context, packageName: String?): Boolean {
        val installerPackageNames = AppInstaller.getAvailableInstallersInfo(context)
            .flatMap { it.installerPackageNames }
            .toSet()
        val packageInstaller = context.packageManager.getUpdateOwnerPackageNameCompat(packageName)
        return installerPackageNames.contains(packageInstaller)
    }

    fun getEncodedCertificateHashes(context: Context, packageName: String): List<String> = try {
        val certificates = getX509Certificates(context, packageName)
        certificates.map {
            val messageDigest = MessageDigest.getInstance(Algorithm.SHA.value)
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

    private fun isSignedByFDroid(context: Context, packageName: String?): Boolean = try {
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

    fun isMicroGGms(context: Context): Boolean {
        return try {
            val packageInfo =
                getPackageInfo(context, PACKAGE_NAME_GMS, PackageManager.GET_PERMISSIONS)
            val hasFakePackageSignature = packageInfo.requestedPermissions?.any { permission ->
                permission == "android.permission.FAKE_PACKAGE_SIGNATURE"
            } == true

            return hasFakePackageSignature
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to check origin for $PACKAGE_NAME_GMS")
            false
        }
    }

    private fun isInstalledByFDroid(context: Context, packageName: String?): Boolean =
        fdroidPackages.contains(
            context.packageManager.getUpdateOwnerPackageNameCompat(packageName)
        )

    private fun getX509Certificates(context: Context, packageName: String?): List<X509Certificate> =
        try {
            val packageInfo = getPackageInfoWithSignature(context, packageName)
            if (isPAndAbove) {
                if (packageInfo.signingInfo!!.hasMultipleSigners()) {
                    packageInfo.signingInfo!!.apkContentsSigners.map {
                        it.generateX509Certificate()
                    }
                } else {
                    packageInfo.signingInfo!!.signingCertificateHistory.map {
                        it.generateX509Certificate()
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures!!.map { it.generateX509Certificate() }
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to get X509 certificates", exception)
            emptyList()
        }

    private fun getPackageInfoWithSignature(context: Context, packageName: String?): PackageInfo =
        if (isPAndAbove) {
            getPackageInfo(context, packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            @Suppress("DEPRECATION")
            getPackageInfo(context, packageName, PackageManager.GET_SIGNATURES)
        }

    private fun extractSHA1Fingerprint(certificate: X509Certificate): String {
        val messageDigest = MessageDigest.getInstance(Algorithm.SHA1.value)
        messageDigest.update(certificate.encoded)
        return messageDigest.digest()
            .joinToString("") { byte -> String.format("%02x", byte) }
            .lowercase()
    }

    private fun parseX500Principal(principal: X500Principal): Map<String, String> =
        principal.name.split(",").associate {
            val (left, right) = it.split("=")
            left.trim() to right.trim()
        }
}
