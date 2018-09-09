/*
 * Copyright (C) 2016 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */

package com.dragons.aurora;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import timber.log.Timber;

/**
 * NOTE: Silly Android API naming: APK signatures are actually certificates!
 * Thus, we are comparing certificates (including the public key) used to sign an APK,
 * not the actual APK signature.
 */
class ApkSignatureVerifier {

    private final PackageManager pm;

    ApkSignatureVerifier(Context context) {
        pm = context.getPackageManager();
    }

    public boolean match(String packageName, File apkFile) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1) {
            // Android bug
            // PackageManager#getPackageArchiveInfo(String, int) does not include signatures if Android version is 2.3.7 and less.
            return true;
        }
        byte[] apkSig = getApkSignature(apkFile);
        byte[] localSig = getLocalSignature(packageName);
        return localSig.length == 0 || Arrays.equals(apkSig, localSig);
    }

    private byte[] getApkSignature(File apkFile) {
        final String pkgPath = apkFile.getAbsolutePath();
        PackageInfo pkgInfo = pm.getPackageArchiveInfo(pkgPath, PackageManager.GET_SIGNATURES);
        if (null == pkgInfo || null == pkgInfo.signatures) {
            return new byte[]{};
        }
        return signatureToBytes(pkgInfo.signatures);
    }

    private byte[] getLocalSignature(String packageName) {
        try {
            // we do check the byte array of *all* signatures
            @SuppressLint("PackageManagerGetSignatures")
            PackageInfo pkgInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
            return signatureToBytes(pkgInfo.signatures);
        } catch (PackageManager.NameNotFoundException e) {
            return new byte[]{};
        }
    }

    private byte[] signatureToBytes(Signature[] signatures) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for (Signature sig : signatures) {
            try {
                outputStream.write(sig.toByteArray());
            } catch (IOException e) {
                Timber.e("Should not happen! Concatenating signatures failed");
            }
        }

        return outputStream.toByteArray();
    }

}
