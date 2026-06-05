/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.room.account

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AppAccountBindingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(binding: AppAccountBinding)

    @Query("SELECT * FROM app_account_binding WHERE packageName = :packageName")
    suspend fun getForPackage(packageName: String): AppAccountBinding?

    @Query("DELETE FROM app_account_binding WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Query("DELETE FROM app_account_binding WHERE accountId = :accountId")
    suspend fun deleteByAccountId(accountId: String)
}
