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
import android.content.pm.PackageManager
import com.aurora.extensions.isPAndAbove
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.*

object CertUtil {
    private const val FDROID = "FDROID"
    private const val GUARDIAN = "GUARDIANPROJECT.INFO"

    private fun getX509Certificates(
        context: Context,
        packageName: String
    ): List<X509Certificate> {
        val certificates: MutableList<X509Certificate> = mutableListOf()
        val packageManager = context.applicationContext.packageManager

        try {

            val packageInfo = if (isPAndAbove()) {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            }
            else {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNATURES
                )
            }

            val certificateFactory = CertificateFactory.getInstance("X509")

            if (isPAndAbove()) {
                packageInfo.signingInfo.apkContentsSigners.forEach {
                    val bytes = it.toByteArray()
                    val inputStream: InputStream = ByteArrayInputStream(bytes)
                    certificates.add(
                        certificateFactory!!.generateCertificate(inputStream) as X509Certificate
                    )
                }
            } else {
                for (i in packageInfo.signatures.indices) {
                    val bytes = packageInfo.signatures[i].toByteArray()
                    val inStream: InputStream = ByteArrayInputStream(bytes)
                    certificates.add(
                        certificateFactory!!.generateCertificate(inStream) as X509Certificate
                    )
                }
            }
        } catch (e: Exception) {

        }

        return certificates
    }

    fun isFDroidApp(context: Context, packageName: String): Boolean {
        val certificates = getX509Certificates(context, packageName)

        return if (certificates.isEmpty())
            false
        else {
            val cert = certificates[0]

            if (cert.subjectDN != null) {
                val DN = cert.subjectDN.name.toUpperCase(Locale.getDefault())
                DN.contains(FDROID) || DN.contains(GUARDIAN)
            } else {
                false
            }
        }
    }
}