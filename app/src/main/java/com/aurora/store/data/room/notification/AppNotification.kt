/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.room.notification

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "notification")
data class AppNotification(
    @PrimaryKey val id: String,
    val type: Type,
    val title: String,
    val body: String,
    val packageName: String? = null,
    val url: String? = null,
    val timestamp: Long,
    val isRead: Boolean = false
) : Parcelable {

    enum class Type {
        REMOTE
    }
}
