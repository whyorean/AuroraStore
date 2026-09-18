/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.room.notification

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: AppNotification)

    @Query("SELECT * FROM notification ORDER BY timestamp DESC")
    fun notifications(): Flow<List<AppNotification>>

    @Query("UPDATE notification SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: String)

    @Query("UPDATE notification SET isRead = 1")
    suspend fun markAllRead()

    @Query("DELETE FROM notification WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM notification")
    suspend fun deleteAll()
}
