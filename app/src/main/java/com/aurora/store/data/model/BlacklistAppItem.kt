/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.model

import android.graphics.Bitmap
import com.aurora.store.compose.ui.commons.InstalledAppMeta

data class BlacklistAppItem(
    override val packageName: String,
    val displayName: String,
    val versionName: String,
    val versionCode: Long,
    val icon: Bitmap,
    val isFiltered: Boolean,
    override val firstInstallTime: Long = 0L,
    override val lastUpdateTime: Long = 0L,
    override val sizeBytes: Long = 0L,
    override val isSystem: Boolean = false,
    override val installer: String? = null
) : InstalledAppMeta {
    override val label: String get() = displayName
}
