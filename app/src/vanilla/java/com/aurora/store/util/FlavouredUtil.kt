/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.util

import android.content.Context
import com.aurora.Constants

object FlavouredUtil : IFlavouredUtil {

    override val defaultDispensers = setOf(Constants.URL_DISPENSER)

    override fun promptMicroGInstall(context: Context): Boolean = false
}
