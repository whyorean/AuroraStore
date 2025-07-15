package com.aurora.store.util

import android.content.Context

object FlavouredUtil : IFlavouredUtil {

    override fun promptMicroGInstall(context: Context): Boolean {
        return false
    }
}