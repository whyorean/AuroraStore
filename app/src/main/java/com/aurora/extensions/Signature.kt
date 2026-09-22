/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.extensions

import android.content.pm.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

fun Signature.generateX509Certificate(): X509Certificate {
    val certificateFactory = CertificateFactory.getInstance("X509")
    return certificateFactory.generateCertificate(
        this.toByteArray().inputStream()
    ) as X509Certificate
}
