/*
 * Aurora Store
 * Copyright (C) 2018  Rahul Kumar Patel <whyorean@gmail.com>
 *
 * ChecKey
 * Copyright (C) 2018 Hans-Christoph Steiner
 *
 * Aurora Store (a fork of Yalp Store )is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Aurora Store is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.dragons.aurora;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public final class CertUtils {

    private static final String FDROID = "FDROID";
    private static final String GUARDIAN = "GUARDIANPROJECT.INFO";

    private static CertificateFactory certificateFactory;

    private static X509Certificate[] getX509Certificates(Context context, String packageName) {
        X509Certificate[] certs = null;
        PackageManager pm = context.getApplicationContext().getPackageManager();
        try {
            @SuppressLint("PackageManagerGetSignatures")
            PackageInfo pkgInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
            if (certificateFactory == null)
                certificateFactory = CertificateFactory.getInstance("X509");
            certs = new X509Certificate[pkgInfo.signatures.length];
            for (int i = 0; i < certs.length; i++) {
                byte[] cert = pkgInfo.signatures[i].toByteArray();
                InputStream inStream = new ByteArrayInputStream(cert);
                certs[i] = (X509Certificate) certificateFactory.generateCertificate(inStream);
            }
        } catch (NameNotFoundException | CertificateException e) {
            e.printStackTrace();
        }
        return certs;
    }

    public static boolean isFDroidApp(Context context, String packageName) {
        X509Certificate[] certs = CertUtils.getX509Certificates(context, packageName);
        if (certs == null || certs.length < 1)
            return false;
        else {
            X509Certificate cert = certs[0];
            String DN = cert.getSubjectDN().getName().toUpperCase();
            if (DN.contains(FDROID) || DN.contains(GUARDIAN))
                return true;
        }
        return false;
    }
}

