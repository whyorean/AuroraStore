package com.aurora.store.util

import android.content.Context
import com.aurora.extensions.isHuawei

object FlavouredUtil : IFlavouredUtil {

    override val defaultDispensers = emptySet()

    override fun promptMicroGInstall(context: Context): Boolean {
        return isHuawei &&
                PackageUtil.hasSupportedAppGallery(context) &&
                !PackageUtil.isMicroGBundleInstalled(context)
    }
}
