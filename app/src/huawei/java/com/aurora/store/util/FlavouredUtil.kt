/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.util

import android.content.Context
import com.aurora.extensions.isHuawei

object FlavouredUtil : IFlavouredUtil {

    override val defaultDispensers: Set<String> = emptySet()

    override fun promptMicroGInstall(context: Context): Boolean = isHuawei &&
        PackageUtil.hasSupportedAppGallery(context) &&
        !PackageUtil.isMicroGBundleInstalled(context)
}
