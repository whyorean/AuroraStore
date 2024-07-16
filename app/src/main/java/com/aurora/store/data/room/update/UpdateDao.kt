package com.aurora.store.data.room.update

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface UpdateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(updates: List<Update>)

    @Query("SELECT * FROM `update` ORDER BY displayName ASC")
    fun updates(): Flow<List<Update>>

    @Query("DELETE FROM `update` WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Query("DELETE FROM `update`")
    suspend fun deleteAll()

    @Transaction
    suspend fun insertUpdates(updates: List<Update>) {
        deleteAll()
        insertAll(updates)
    }
}
