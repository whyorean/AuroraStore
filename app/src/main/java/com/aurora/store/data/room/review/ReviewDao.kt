/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.room.review

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(review: LocalReview)

    @Query("SELECT * FROM review WHERE packageName = :packageName AND accountEmail = :accountEmail")
    fun review(packageName: String, accountEmail: String): Flow<LocalReview?>

    @Query("SELECT * FROM review WHERE packageName = :packageName AND accountEmail = :accountEmail")
    suspend fun get(packageName: String, accountEmail: String): LocalReview?

    @Query("DELETE FROM review WHERE packageName = :packageName AND accountEmail = :accountEmail")
    suspend fun delete(packageName: String, accountEmail: String)

    @Query("DELETE FROM review")
    suspend fun deleteAll()
}
