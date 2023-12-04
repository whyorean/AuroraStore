package com.aurora.store.data.room.download

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: Download)

    @Update
    suspend fun update(download: Download)

    @Query("SELECT * FROM download")
    fun downloads(): Flow<List<Download>>

    @Query("SELECT * FROM download WHERE packageName = :packageName")
    suspend fun getDownload(packageName: String): Download?

    @Query("DELETE FROM download WHERE packageName = :packageName")
    suspend fun delete(packageName: String)
}
