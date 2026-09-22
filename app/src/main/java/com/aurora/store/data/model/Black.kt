/*
 * SPDX-FileCopyrightText: 2021 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.model

import android.graphics.drawable.Drawable

data class Black(val packageName: String) {
    var displayName: String = String()
    var drawable: Drawable? = null
    var versionName: String = String()
    var versionCode: Long = 0

    override fun hashCode(): Int = packageName.hashCode()

    override fun equals(other: Any?): Boolean = when (other) {
        is Black -> other.packageName == packageName
        else -> false
    }
}
