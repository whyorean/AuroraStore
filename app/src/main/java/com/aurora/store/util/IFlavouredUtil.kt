/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.util

import android.content.Context

interface IFlavouredUtil {
    val defaultDispensers: Set<String>
    fun promptMicroGInstall(context: Context): Boolean
}
