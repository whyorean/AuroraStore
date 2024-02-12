package com.aurora.extensions

import android.content.Context
import android.content.pm.PackageManager
import com.aurora.gplayapi.data.models.App
import com.aurora.store.util.CertUtil

fun App.isEnabled(context: Context): Boolean {
    return context.packageManager.getApplicationInfo(
        this.packageName,
        PackageManager.GET_META_DATA
    ).enabled
}

fun App.hasValidCertificate(context: Context): Boolean {
    return this.certificateSetList.any {
        it.certificateSet in CertUtil.getEncodedCertificateHashes(
            context,
            this.packageName
        )
    }
}
