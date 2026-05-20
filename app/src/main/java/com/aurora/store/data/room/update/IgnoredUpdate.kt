/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.room.update

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Marks an update as ignored by the user.
 *
 * @property packageName Package whose updates are being ignored.
 * @property ignoredVersionCode When `null`, all future updates for this package are hidden.
 *   When set, only that specific version is hidden — a newer version arriving will show up
 *   in the Updates list again.
 */
@Entity(tableName = "ignored_update")
data class IgnoredUpdate(
    @PrimaryKey
    val packageName: String,
    val ignoredVersionCode: Long? = null
)
