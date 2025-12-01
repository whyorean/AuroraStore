package com.aurora.store.util

import android.content.Context

interface IFlavouredUtil {
    val defaultDispensers: Set<String>
    fun promptMicroGInstall(context: Context): Boolean
}
