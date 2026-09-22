/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.model

data class StorageRequirement(
    val required: Long,
    val available: Long,
    val appName: String? = null,
    val appCount: Int = 1
) {
    val isSufficient: Boolean get() = available >= required

    val shortfall: Long get() = (required - available).coerceAtLeast(0)
}
